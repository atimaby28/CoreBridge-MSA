package halo.corebridge.apply.consumer;

import halo.corebridge.apply.client.NotificationClient;
import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.service.ProcessService;
import halo.corebridge.common.dataserializer.DataSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyEventConsumer 테스트")
class ApplyEventConsumerTest {

    @InjectMocks
    private ApplyEventConsumer applyEventConsumer;

    @Mock
    private ApplyRepository applyRepository;

    @Mock
    private ProcessService processService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Nested
    @DisplayName("consume() - Kafka 메시지 소비")
    class ConsumeTest {

        @Test
        @DisplayName("성공: 정상 메시지를 소비하여 Apply와 Process를 DB에 저장한다")
        void consume_validMessage_savesApplyAndProcess() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .coverLetter("테스트")
                    .build();

            try (MockedStatic<DataSerializer> ds = mockStatic(DataSerializer.class)) {
                ds.when(() -> DataSerializer.deserialize(anyString(), eq(ApplyDto.CreateRequest.class)))
                        .thenReturn(request);

                given(applyRepository.existsByJobpostingIdAndUserId(100L, 200L)).willReturn(false);
                given(applyRepository.save(any(Apply.class))).willAnswer(inv -> inv.getArgument(0));
                given(processService.createProcess(anyLong(), eq(100L), eq(200L)))
                        .willReturn(RecruitmentProcess.create(1L, 100L, 200L, 300L));

                // when
                applyEventConsumer.consume("any-message");

                // then
                verify(applyRepository).save(any(Apply.class));
                verify(processService).createProcess(anyLong(), eq(100L), eq(200L));
                verify(notificationClient).sendProcessNotification(eq(200L), any(), anyLong(), eq(100L));
            }
        }

        @Test
        @DisplayName("성공: DB 중복 감지 시 저장을 스킵한다")
        void consume_duplicateInDb_skips() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            try (MockedStatic<DataSerializer> ds = mockStatic(DataSerializer.class)) {
                ds.when(() -> DataSerializer.deserialize(anyString(), eq(ApplyDto.CreateRequest.class)))
                        .thenReturn(request);

                given(applyRepository.existsByJobpostingIdAndUserId(100L, 200L)).willReturn(true);

                // when
                applyEventConsumer.consume("any-message");

                // then
                verify(applyRepository, never()).save(any());
                verify(processService, never()).createProcess(anyLong(), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("성공: 역직렬화 실패 시 예외 없이 로그만 남긴다")
        void consume_invalidMessage_doesNotThrow() {
            // given — DataSerializer가 null을 반환하는 경우
            try (MockedStatic<DataSerializer> ds = mockStatic(DataSerializer.class)) {
                ds.when(() -> DataSerializer.deserialize(anyString(), eq(ApplyDto.CreateRequest.class)))
                        .thenReturn(null);

                // when
                applyEventConsumer.consume("invalid json");

                // then
                verify(applyRepository, never()).save(any());
                verify(applyRepository, never()).existsByJobpostingIdAndUserId(any(), any());
            }
        }

        @Test
        @DisplayName("성공: DB 저장 실패 시 Redis에서 제거하여 재지원 가능하게 한다")
        void consume_dbSaveFail_rollbackRedis() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            try (MockedStatic<DataSerializer> ds = mockStatic(DataSerializer.class)) {
                ds.when(() -> DataSerializer.deserialize(anyString(), eq(ApplyDto.CreateRequest.class)))
                        .thenReturn(request);

                given(applyRepository.existsByJobpostingIdAndUserId(100L, 200L)).willReturn(false);
                given(applyRepository.save(any(Apply.class))).willThrow(new RuntimeException("DB error"));
                given(redisTemplate.opsForSet()).willReturn(setOperations);

                // when
                applyEventConsumer.consume("any-message");

                // then — Redis 롤백 검증
                verify(setOperations).remove("applied:100", "200");
            }
        }
    }
}
