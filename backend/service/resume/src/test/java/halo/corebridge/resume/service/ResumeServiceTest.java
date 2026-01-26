package halo.corebridge.resume.service;

import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.entity.ResumeVersion;
import halo.corebridge.resume.model.enums.ResumeStatus;
import halo.corebridge.resume.repository.ResumeRepository;
import halo.corebridge.resume.repository.ResumeVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @InjectMocks
    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    private Resume testResume;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        testResume = Resume.create(USER_ID, "백엔드 개발자 이력서", "경력 사항...");
        setField(testResume, "id", 1L);
    }

    @Nested
    @DisplayName("이력서 조회/생성")
    class GetOrCreateTests {

        @Test
        @DisplayName("성공: 기존 이력서 조회")
        void getOrCreate_existingResume() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));

            // when
            ResumeDto.ResumeResponse result = resumeService.getOrCreate(USER_ID);

            // then
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getTitle()).isEqualTo("백엔드 개발자 이력서");
            verify(resumeRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공: 이력서 없으면 자동 생성")
        void getOrCreate_createNew() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(resumeRepository.save(any(Resume.class))).willAnswer(invocation -> {
                Resume saved = invocation.getArgument(0);
                setField(saved, "id", 1L);
                return saved;
            });

            // when
            ResumeDto.ResumeResponse result = resumeService.getOrCreate(USER_ID);

            // then
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getTitle()).isEqualTo("내 이력서");
            assertThat(result.getStatus()).isEqualTo(ResumeStatus.DRAFT);
            verify(resumeRepository, times(1)).save(any(Resume.class));
        }
    }

    @Nested
    @DisplayName("이력서 업데이트")
    class UpdateTests {

        @Test
        @DisplayName("성공: 이력서 업데이트 (버전 스냅샷 저장)")
        void update_success() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));
            given(resumeRepository.save(any(Resume.class))).willAnswer(inv -> inv.getArgument(0));
            given(versionRepository.save(any(ResumeVersion.class))).willAnswer(inv -> inv.getArgument(0));

            ResumeDto.UpdateRequest request = new ResumeDto.UpdateRequest();
            setField(request, "title", "수정된 이력서");
            setField(request, "content", "수정된 내용");

            int originalVersion = testResume.getCurrentVersion();

            // when
            ResumeDto.ResumeResponse result = resumeService.update(USER_ID, request);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 이력서");
            assertThat(result.getCurrentVersion()).isEqualTo(originalVersion + 1);
            verify(versionRepository, times(1)).save(any(ResumeVersion.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void update_userNotFound() {
            // given
            given(resumeRepository.findByUserId(999L)).willReturn(Optional.empty());

            ResumeDto.UpdateRequest request = new ResumeDto.UpdateRequest();
            setField(request, "title", "제목");

            // when & then
            assertThatThrownBy(() -> resumeService.update(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이력서를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionTests {

        @Test
        @DisplayName("성공: 버전 목록 조회")
        void getVersions_success() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));
            
            ResumeVersion v1 = ResumeVersion.create(1L, 1, "제목1", "내용1");
            ResumeVersion v2 = ResumeVersion.create(1L, 2, "제목2", "내용2");
            given(versionRepository.findByResumeIdOrderByVersionDesc(1L))
                    .willReturn(List.of(v2, v1));

            // when
            ResumeDto.VersionListResponse result = resumeService.getVersions(USER_ID);

            // then
            assertThat(result.getVersions()).hasSize(2);
            assertThat(result.getVersions().get(0).getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공: 특정 버전으로 복원")
        void restoreVersion_success() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));
            
            ResumeVersion targetVersion = ResumeVersion.create(1L, 1, "이전 제목", "이전 내용");
            given(versionRepository.findByResumeIdAndVersion(1L, 1))
                    .willReturn(Optional.of(targetVersion));
            given(versionRepository.save(any(ResumeVersion.class))).willAnswer(inv -> inv.getArgument(0));
            given(resumeRepository.save(any(Resume.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ResumeDto.ResumeResponse result = resumeService.restoreVersion(USER_ID, 1);

            // then
            assertThat(result.getTitle()).isEqualTo("이전 제목");
            assertThat(result.getContent()).isEqualTo("이전 내용");
            verify(versionRepository, times(1)).save(any(ResumeVersion.class)); // 백업 저장
        }

        @Test
        @DisplayName("실패: 존재하지 않는 버전")
        void restoreVersion_versionNotFound() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));
            given(versionRepository.findByResumeIdAndVersion(1L, 999))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> resumeService.restoreVersion(USER_ID, 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("AI 분석")
    class AiAnalysisTests {

        @Test
        @DisplayName("성공: AI 분석 요청")
        void requestAnalysis_success() {
            // given
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(testResume));
            given(resumeRepository.save(any(Resume.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ResumeDto.ResumeResponse result = resumeService.requestAnalysis(USER_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ResumeStatus.ANALYZING);
            verify(resumeRepository, times(1)).save(any(Resume.class));
        }

        @Test
        @DisplayName("실패: 내용 없이 분석 요청")
        void requestAnalysis_noContent() {
            // given
            Resume emptyResume = Resume.create(USER_ID);
            given(resumeRepository.findByUserId(USER_ID)).willReturn(Optional.of(emptyResume));

            // when & then
            assertThatThrownBy(() -> resumeService.requestAnalysis(USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이력서 내용이 없습니다");
        }

        @Test
        @DisplayName("성공: AI 분석 결과 저장")
        void updateAiResult_success() {
            // given
            given(resumeRepository.findById(1L)).willReturn(Optional.of(testResume));
            given(resumeRepository.save(any(Resume.class))).willAnswer(inv -> inv.getArgument(0));

            ResumeDto.AiResultRequest request = new ResumeDto.AiResultRequest();
            setField(request, "summary", "5년차 백엔드 개발자");
            setField(request, "skills", "[\"Java\", \"Spring\"]");
            setField(request, "experienceYears", 5);

            // when
            ResumeDto.ResumeResponse result = resumeService.updateAiResult(1L, request);

            // then
            assertThat(result.getStatus()).isEqualTo(ResumeStatus.ANALYZED);
            assertThat(result.getAiSummary()).isEqualTo("5년차 백엔드 개발자");
            assertThat(result.getAiSkills()).containsExactly("Java", "Spring");
            assertThat(result.getAiExperienceYears()).isEqualTo(5);
        }
    }

    // 리플렉션 헬퍼
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            try {
                var field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set field: " + fieldName, ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
