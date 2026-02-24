package halo.corebridge.apply.service;

import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.repository.RecruitmentProcessRepository;
import halo.corebridge.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ApplyService 단위 테스트
 *
 * [비동기 지원 구조 반영 — v2]
 * apply()는 DB를 거치지 않고 Redis SADD + Kafka 발행만 수행합니다.
 * 실제 DB 저장은 ApplyEventConsumer가 백그라운드에서 처리합니다.
 *
 * 변경 이력:
 * - v1: applyRepository 기반 동기 지원 테스트
 * - v2: Redis + Kafka 비동기 전환에 맞춰 테스트 리팩토링
 */
@ExtendWith(MockitoExtension.class)
class ApplyServiceTest {

    @InjectMocks
    private ApplyService applyService;

    @Mock
    private ApplyRepository applyRepository;

    @Mock
    private RecruitmentProcessRepository processRepository;

    @Mock
    private ProcessService processService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private Apply testApply;
    private RecruitmentProcess testProcess;

    @BeforeEach
    void setUp() {
        testApply = Apply.create(1L, 100L, 200L, 300L, "자기소개서입니다.");
        testProcess = RecruitmentProcess.create(10L, 1L, 100L, 200L);
    }

    // ============================================
    // 지원하기 (비동기: Redis SADD + Kafka 발행)
    // ============================================

    @Nested
    @DisplayName("지원하기 (비동기)")
    class ApplyTests {

        @Test
        @DisplayName("성공: Redis 중복 체크 통과 → Kafka 발행 → 즉시 ACCEPTED 응답")
        void apply_success() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .coverLetter("열심히 하겠습니다.")
                    .build();

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add("applied:100", "200")).willReturn(1L); // 신규 지원
            given(kafkaTemplate.send(eq("corebridge-apply"), eq("100"), anyString()))
                    .willReturn(new CompletableFuture<>());

            // when
            ApplyDto.ApplyAcceptedResponse result = applyService.apply(request);

            // then — 즉시 응답 검증
            assertThat(result.getJobpostingId()).isEqualTo(100L);
            assertThat(result.getUserId()).isEqualTo(200L);
            assertThat(result.getStatus()).isEqualTo("ACCEPTED");

            // DB 접근 없음 검증 (비동기 처리는 Consumer 담당)
            verify(applyRepository, never()).save(any());
            verify(applyRepository, never()).existsByJobpostingIdAndUserId(any(), any());
            verify(kafkaTemplate, times(1)).send(eq("corebridge-apply"), eq("100"), anyString());
        }

        @Test
        @DisplayName("실패: Redis SADD 0 반환 → 중복 지원 예외")
        void apply_duplicate_throwsException() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add("applied:100", "200")).willReturn(0L); // 이미 존재

            // when & then
            assertThatThrownBy(() -> applyService.apply(request))
                    .isInstanceOf(BaseException.class);

            // Kafka 발행 시도조차 안 함
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패: Kafka 발행 실패 → Redis 롤백 (재지원 가능)")
        void apply_kafkaFail_rollbackRedis() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add("applied:100", "200")).willReturn(1L); // 신규 통과
            given(kafkaTemplate.send(eq("corebridge-apply"), eq("100"), anyString()))
                    .willThrow(new RuntimeException("Kafka broker unavailable"));

            // when & then
            assertThatThrownBy(() -> applyService.apply(request))
                    .isInstanceOf(BaseException.class);

            // Redis 롤백 검증 → 유저가 재지원할 수 있도록
            verify(setOperations, times(1)).remove("applied:100", "200");
        }

        @Test
        @DisplayName("실패: Redis SADD null 반환 → 중복 지원 예외")
        void apply_redisNull_throwsException() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add("applied:100", "200")).willReturn(null); // Redis 이상

            // when & then
            assertThatThrownBy(() -> applyService.apply(request))
                    .isInstanceOf(BaseException.class);

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    // ============================================
    // 지원 취소
    // ============================================

    @Nested
    @DisplayName("지원 취소")
    class CancelTests {

        @Test
        @DisplayName("성공: APPLIED 상태에서 취소 → DB 삭제 + Redis 제거")
        void cancel_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));
            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            applyService.cancel(1L, 200L);

            // then — DB 삭제 검증
            verify(processRepository, times(1)).delete(testProcess);
            verify(applyRepository, times(1)).delete(testApply);

            // Redis에서도 제거 → 재지원 가능하도록
            verify(setOperations, times(1)).remove("applied:100", "200");
        }

        @Test
        @DisplayName("실패: 본인이 아닌 경우")
        void cancel_notOwner_throwsException() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));

            // when & then
            assertThatThrownBy(() -> applyService.cancel(1L, 999L))
                    .isInstanceOf(BaseException.class);

            verify(processRepository, never()).delete(any());
            verify(applyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 진행 중인 상태에서 취소 시도")
        void cancel_inProgress_throwsException() {
            // given
            testProcess.transition(ProcessStep.DOCUMENT_REVIEW);
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when & then
            assertThatThrownBy(() -> applyService.cancel(1L, 200L))
                    .isInstanceOf(BaseException.class);

            verify(processRepository, never()).delete(any());
            verify(applyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 지원")
        void cancel_notFound_throwsException() {
            // given
            given(applyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applyService.cancel(999L, 200L))
                    .isInstanceOf(BaseException.class);
        }
    }

    // ============================================
    // 지원 조회
    // ============================================

    @Nested
    @DisplayName("지원 조회")
    class ReadTests {

        @Test
        @DisplayName("성공: 지원 상세 조회")
        void read_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyDetailResponse result = applyService.read(1L);

            // then
            assertThat(result.getApplyId()).isEqualTo(1L);
            assertThat(result.getJobpostingId()).isEqualTo(100L);
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.APPLIED);
        }

        @Test
        @DisplayName("성공: 내 지원 목록 조회")
        void getMyApplies_success() {
            // given
            List<Apply> applies = List.of(testApply);
            given(applyRepository.findByUserIdOrderByCreatedAtDesc(200L)).willReturn(applies);
            given(applyRepository.countByUserId(200L)).willReturn(1L);
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getMyApplies(200L);

            // then
            assertThat(result.getApplies()).hasSize(1);
            assertThat(result.getTotalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: 공고별 지원자 목록 조회")
        void getAppliesByJobposting_success() {
            // given
            List<Apply> applies = List.of(testApply);
            given(applyRepository.findByJobpostingIdOrderByCreatedAtDesc(100L)).willReturn(applies);
            given(applyRepository.countByJobpostingId(100L)).willReturn(1L);
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getAppliesByJobposting(100L);

            // then
            assertThat(result.getApplies()).hasSize(1);
            assertThat(result.getTotalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: 공고별 특정 단계 지원자 목록 조회")
        void getAppliesByStep_success() {
            // given
            List<RecruitmentProcess> processes = List.of(testProcess);
            given(processRepository.findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(100L, ProcessStep.APPLIED))
                    .willReturn(processes);
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getAppliesByStep(100L, ProcessStep.APPLIED);

            // then
            assertThat(result.getApplies()).hasSize(1);
        }
    }

    // ============================================
    // 메모 수정
    // ============================================

    @Nested
    @DisplayName("메모 수정")
    class UpdateMemoTests {

        @Test
        @DisplayName("성공: 메모 수정")
        void updateMemo_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            ApplyDto.UpdateMemoRequest request = ApplyDto.UpdateMemoRequest.builder()
                    .memo("면접 시 추가 질문 필요")
                    .build();

            // when
            ApplyDto.ApplyDetailResponse result = applyService.updateMemo(1L, request);

            // then
            assertThat(result.getMemo()).isEqualTo("면접 시 추가 질문 필요");
        }
    }
}
