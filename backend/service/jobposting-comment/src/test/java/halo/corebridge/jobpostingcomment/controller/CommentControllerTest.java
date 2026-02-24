package halo.corebridge.jobpostingcomment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostingcomment.config.SecurityConfig;
import halo.corebridge.jobpostingcomment.model.dto.CommentDto;
import halo.corebridge.jobpostingcomment.service.CommentService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentController 테스트")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private CommentDto.CommentResponse createTestComment() {
        return CommentDto.CommentResponse.builder()
                .commentId(1L)
                .content("좋은 공고네요!")
                .userId(100L)
                .jobpostingId(200L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/comments/{commentId} - 댓글 단건 조회")
    class ReadTest {

        @Test
        @DisplayName("성공: 댓글을 조회한다")
        void read_success() throws Exception {
            // given
            given(commentService.read(1L)).willReturn(createTestComment());

            // when & then
            mockMvc.perform(get("/api/v1/comments/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.commentId").value(1))
                    .andExpect(jsonPath("$.result.content").value("좋은 공고네요!"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/comments - 댓글 목록 조회")
    class ReadAllTest {

        @Test
        @DisplayName("성공: 공고별 댓글 목록을 조회한다")
        void readAll_success() throws Exception {
            // given
            CommentDto.CommentPageResponse pageResponse = CommentDto.CommentPageResponse.builder()
                    .comments(List.of(createTestComment()))
                    .commentCount(1L)
                    .build();
            given(commentService.readAll(200L, 1L, 10L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/comments")
                            .param("jobpostingId", "200")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.comments").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/comments - 댓글 작성")
    class CreateTest {

        @Test
        @DisplayName("성공: 댓글을 작성한다")
        void create_success() throws Exception {
            // given
            String requestBody = """
                {
                    "content": "좋은 공고네요!",
                    "jobpostingId": 200
                }
                """;
            given(commentService.create(any(), any())).willReturn(createTestComment());

            // when & then
            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.content").value("좋은 공고네요!"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/comments/{commentId} - 댓글 삭제")
    class DeleteTest {

        @Test
        @DisplayName("성공: 댓글을 삭제한다")
        void delete_success() throws Exception {
            // given
            doNothing().when(commentService).delete(eq(1L), any());

            // when & then
            mockMvc.perform(delete("/api/v1/comments/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
