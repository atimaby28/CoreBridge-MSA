package halo.corebridge.jobpostingread.service;

import halo.corebridge.jobpostingread.client.*;
import halo.corebridge.jobpostingread.model.dto.JobpostingReadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JobpostingReadServiceTest {

    @Mock
    private JobpostingClient jobpostingClient;
    @Mock
    private ViewClient viewClient;
    @Mock
    private LikeClient likeClient;
    @Mock
    private CommentClient commentClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private JobpostingReadService jobpostingReadService;

    private JobpostingClient.JobpostingResponse mockJobposting;

    @BeforeEach
    void setUp() {
        mockJobposting = new JobpostingClient.JobpostingResponse();
        mockJobposting.setJobpostingId(1L);
        mockJobposting.setTitle("테스트 채용공고");
        mockJobposting.setContent("테스트 내용");
        mockJobposting.setBoardId(1L);
        mockJobposting.setUserId(100L);
        mockJobposting.setCreatedAt(LocalDateTime.now());
        mockJobposting.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("단일 채용공고 조회 - 성공")
    void read_success() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingClient.read(jobpostingId)).willReturn(mockJobposting);
        given(viewClient.count(jobpostingId)).willReturn(100L);
        given(likeClient.count(jobpostingId)).willReturn(50L);
        given(commentClient.count(jobpostingId)).willReturn(10L);
        given(userClient.getNickname(100L)).willReturn("테스터");

        // when
        JobpostingReadDto.Response response = jobpostingReadService.read(jobpostingId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getJobpostingId()).isEqualTo(jobpostingId);
        assertThat(response.getTitle()).isEqualTo("테스트 채용공고");
        assertThat(response.getViewCount()).isEqualTo(100L);
        assertThat(response.getLikeCount()).isEqualTo(50L);
        assertThat(response.getCommentCount()).isEqualTo(10L);
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("단일 채용공고 조회 - 존재하지 않는 경우")
    void read_notFound() {
        // given
        Long jobpostingId = 999L;
        given(jobpostingClient.read(jobpostingId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> jobpostingReadService.read(jobpostingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Jobposting not found");
    }

    @Test
    @DisplayName("채용공고 목록 조회 - 성공")
    void readAll_success() {
        // given
        Long boardId = 1L;
        Long page = 1L;
        Long pageSize = 10L;

        JobpostingClient.JobpostingPageResponse pageResponse = new JobpostingClient.JobpostingPageResponse();
        pageResponse.setJobpostings(List.of(mockJobposting));
        pageResponse.setJobpostingCount(1L);

        given(jobpostingClient.readAll(boardId, page, pageSize)).willReturn(pageResponse);
        given(viewClient.count(1L)).willReturn(100L);
        given(likeClient.count(1L)).willReturn(50L);
        given(commentClient.count(1L)).willReturn(10L);
        given(userClient.getNickname(100L)).willReturn("테스터");

        // when
        JobpostingReadDto.PageResponse response = jobpostingReadService.readAll(boardId, page, pageSize);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getJobpostings()).hasSize(1);
        assertThat(response.getJobpostingCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("채용공고 목록 조회 - 빈 결과")
    void readAll_empty() {
        // given
        Long boardId = 1L;
        Long page = 1L;
        Long pageSize = 10L;

        given(jobpostingClient.readAll(boardId, page, pageSize)).willReturn(null);

        // when
        JobpostingReadDto.PageResponse response = jobpostingReadService.readAll(boardId, page, pageSize);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getJobpostings()).isEmpty();
        assertThat(response.getJobpostingCount()).isEqualTo(0L);
    }
}
