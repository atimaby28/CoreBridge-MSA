package halo.corebridge.resume.service;

import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.repository.ResumeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalysisResultService 테스트")
class AiAnalysisResultServiceTest {

    @InjectMocks
    private AiAnalysisResultService aiAnalysisResultService;

    @Mock
    private ResumeRepository resumeRepository;

    @Nested
    @DisplayName("saveResult() - AI 분석 결과 저장")
    class SaveResultTest {

        @Test
        @DisplayName("성공: AI 분석 결과를 이력서에 저장한다")
        void saveResult_success() {
            // given
            Resume resume = mock(Resume.class);
            given(resumeRepository.findById(1L)).willReturn(Optional.of(resume));

            List<String> skills = List.of("Java", "Spring", "Kubernetes");
            String summary = "5년차 백엔드 개발자";

            // when
            aiAnalysisResultService.saveResult(1L, summary, skills);

            // then
            verify(resume).updateAiAnalysis(eq(summary), eq("[\"Java\",\"Spring\",\"Kubernetes\"]"));
            verify(resumeRepository).save(resume);
        }

        @Test
        @DisplayName("성공: skills가 null이면 aiSkills를 null로 저장한다")
        void saveResult_nullSkills_savesNull() {
            // given
            Resume resume = mock(Resume.class);
            given(resumeRepository.findById(1L)).willReturn(Optional.of(resume));

            // when
            aiAnalysisResultService.saveResult(1L, "요약", null);

            // then
            verify(resume).updateAiAnalysis(eq("요약"), isNull());
            verify(resumeRepository).save(resume);
        }

        @Test
        @DisplayName("성공: skills가 빈 리스트면 aiSkills를 null로 저장한다")
        void saveResult_emptySkills_savesNull() {
            // given
            Resume resume = mock(Resume.class);
            given(resumeRepository.findById(1L)).willReturn(Optional.of(resume));

            // when
            aiAnalysisResultService.saveResult(1L, "요약", List.of());

            // then
            verify(resume).updateAiAnalysis(eq("요약"), isNull());
            verify(resumeRepository).save(resume);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이력서 → 예외를 catch하고 로그만 남긴다")
        void saveResult_resumeNotFound_doesNotThrow() {
            // given
            given(resumeRepository.findById(999L)).willReturn(Optional.empty());

            // when — 예외가 전파되지 않아야 한다
            aiAnalysisResultService.saveResult(999L, "요약", List.of("Java"));

            // then
            verify(resumeRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: DB 저장 실패 시에도 예외가 전파되지 않는다")
        void saveResult_dbError_doesNotThrow() {
            // given
            Resume resume = mock(Resume.class);
            given(resumeRepository.findById(1L)).willReturn(Optional.of(resume));
            doThrow(new RuntimeException("DB error")).when(resumeRepository).save(any());

            // when — 예외가 전파되지 않아야 한다
            aiAnalysisResultService.saveResult(1L, "요약", List.of("Java"));

            // then — catch 블록에서 처리됨
        }
    }
}
