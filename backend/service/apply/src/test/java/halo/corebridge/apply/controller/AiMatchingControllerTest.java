package halo.corebridge.apply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.apply.model.dto.AiMatchingDto;
import halo.corebridge.apply.service.AiMatchingService;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.apply.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AiMatchingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AiMatchingController 테스트")
class AiMatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiMatchingService aiMatchingService;

    @Nested
    @DisplayName("POST /api/v1/ai-matching/match - 후보자 매칭")
    class MatchCandidatesTest {

        @Test
        @DisplayName("성공: 후보자 매칭 결과를 반환한다")
        void matchCandidates_success() throws Exception {
            // given
            AiMatchingDto.MatchCandidatesRequest request =
                    new AiMatchingDto.MatchCandidatesRequest("Java 백엔드 개발자", List.of("Java", "Spring"), 5);

            AiMatchingDto.MatchCandidatesResponse response = AiMatchingDto.MatchCandidatesResponse.builder()
                    .matches(List.of(
                            AiMatchingDto.MatchedCandidate.builder()
                                    .candidateId("C001")
                                    .score(0.92)
                                    .skills(List.of("Java", "Spring", "Kafka"))
                                    .build()
                    ))
                    .totalCount(1)
                    .build();

            given(aiMatchingService.matchCandidates(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/match")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(1))
                    .andExpect(jsonPath("$.result.matches[0].candidateId").value("C001"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai-matching/score - 스코어 계산")
    class ScoreCandidateTest {

        @Test
        @DisplayName("성공: 스코어 계산 결과를 반환한다")
        void scoreCandidate_success() throws Exception {
            // given
            AiMatchingDto.ScoreRequest request =
                    new AiMatchingDto.ScoreRequest("C001", "Java 백엔드", List.of("Java", "Spring"));

            AiMatchingDto.ScoreResponse response = AiMatchingDto.ScoreResponse.builder()
                    .candidateId("C001")
                    .cosineSimilarity(0.85)
                    .candidateSkills(List.of("Java", "Spring", "Kafka"))
                    .requiredSkills(List.of("Java", "Spring"))
                    .scoreDetail(AiMatchingDto.ScoreDetail.builder()
                            .totalScore(87.5)
                            .grade("A")
                            .build())
                    .build();

            given(aiMatchingService.scoreCandidate(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/score")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.candidateId").value("C001"))
                    .andExpect(jsonPath("$.result.scoreDetail.grade").value("A"));
        }

        @Test
        @DisplayName("실패: AI 서비스 실패 시 500을 반환한다")
        void scoreCandidate_aiFailure_returns500() throws Exception {
            // given
            AiMatchingDto.ScoreRequest request =
                    new AiMatchingDto.ScoreRequest("C001", "Java 백엔드", List.of("Java"));

            given(aiMatchingService.scoreCandidate(any())).willReturn(null);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/score")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai-matching/match-jobpostings - 채용공고 추천")
    class MatchJobpostingsTest {

        @Test
        @DisplayName("성공: 채용공고 추천 결과를 반환한다")
        void matchJobpostings_success() throws Exception {
            // given
            AiMatchingDto.MatchJobpostingsRequest request =
                    new AiMatchingDto.MatchJobpostingsRequest("5년차 백엔드 개발자", 3);

            AiMatchingDto.MatchJobpostingsResponse response = AiMatchingDto.MatchJobpostingsResponse.builder()
                    .matches(List.of(
                            AiMatchingDto.MatchedJobposting.builder()
                                    .jobpostingId("JP100")
                                    .title("시니어 백엔드 개발자")
                                    .score(0.88)
                                    .build()
                    ))
                    .totalCount(1)
                    .build();

            given(aiMatchingService.matchJobpostings(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/match-jobpostings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.matches[0].title").value("시니어 백엔드 개발자"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai-matching/skill-gap - 스킬 갭 분석")
    class SkillGapTest {

        @Test
        @DisplayName("성공: 스킬 갭 분석 결과를 반환한다")
        void analyzeSkillGap_success() throws Exception {
            // given
            AiMatchingDto.SkillGapRequest request = new AiMatchingDto.SkillGapRequest("C001", "JP100");

            AiMatchingDto.SkillGapResponse response = AiMatchingDto.SkillGapResponse.builder()
                    .candidateId("C001")
                    .jobpostingId("JP100")
                    .candidateSkills(List.of("Java", "Spring"))
                    .requiredSkills(List.of("Java", "Spring", "Kafka"))
                    .matchedSkills(List.of("Java", "Spring"))
                    .missingSkills(List.of("Kafka"))
                    .matchRate(0.67)
                    .cosineSimilarity(0.80)
                    .build();

            given(aiMatchingService.analyzeSkillGap(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/skill-gap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.missingSkills[0]").value("Kafka"))
                    .andExpect(jsonPath("$.result.matchRate").value(0.67));
        }

        @Test
        @DisplayName("실패: AI 서비스 실패 시 500을 반환한다")
        void analyzeSkillGap_aiFailure_returns500() throws Exception {
            // given
            AiMatchingDto.SkillGapRequest request = new AiMatchingDto.SkillGapRequest("C001", "JP100");

            given(aiMatchingService.analyzeSkillGap(any())).willReturn(null);

            // when & then
            mockMvc.perform(post("/api/v1/ai-matching/skill-gap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }
}
