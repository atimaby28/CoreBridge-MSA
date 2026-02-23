package halo.corebridge.jobpostingcomment.api;

import halo.corebridge.jobpostingcomment.model.dto.CommentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CommentApiTest {
    RestClient restClient = RestClient.create("http://localhost:8003");

    @Test
    void create() {
        CommentDto.CommentResponse response1 = createComment(new CommentCreateRequest(1L, "my comment1", null, 1L));
        CommentDto.CommentResponse response2 = createComment(new CommentCreateRequest(1L, "my comment2", response1.getCommentId(), 1L));
        CommentDto.CommentResponse response3 = createComment(new CommentCreateRequest(1L, "my comment3", response1.getCommentId(), 1L));

        System.out.println("commentId=%s".formatted(response1.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response2.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response3.getCommentId()));

//        commentId=123694721668214784
//          commentId=123694721986981888
//          commentId=123694722045702144
    }

    CommentDto.CommentResponse createComment(CommentCreateRequest request) {
        return restClient.post()
                .uri("/api/v1/comments")
                .body(request)
                .retrieve()
                .body(CommentDto.CommentResponse.class);
    }

    @Test
    void read() {
        CommentDto.CommentResponse response = restClient.get()
                .uri("/api/v1/comments/{commentId}", 8064346049155072L)
                .retrieve()
                .body(CommentDto.CommentResponse.class);

        System.out.println("response = " + response);
    }

    @Test
    void delete() {
        //        commentId=2805380614746112 - x
        //          commentId=2805380967067648 - x
        //          commentId=2805380996427776 - x

        restClient.delete()
                .uri("/api/v1/comments/{commentId}", 8064346049155072L)
                .retrieve()
                .toBodilessEntity();
    }

    @Test
    void readAll() {
        CommentDto.CommentPageResponse response = restClient.get()
                .uri("/api/v1/comments?articleId=1&page=1&pageSize=10")
                .retrieve()
                .body(CommentDto.CommentPageResponse.class);

        System.out.println("response.getCommentCount() = " + response.getCommentCount());
        for (CommentDto.CommentResponse comment : response.getComments()) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print("\t");
            }
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }

        /**
         * 1번 페이지 수행 결과
         * comment.getCommentId() = 123693535103893504
         * 	comment.getCommentId() = 123693535468797952
         * 	comment.getCommentId() = 123693535527518208
         * comment.getCommentId() = 123696314740150272
         * 	comment.getCommentId() = 123696314773704717
         * comment.getCommentId() = 123696314740150273
         * 	comment.getCommentId() = 123696314777899028
         * comment.getCommentId() = 123696314740150274
         * 	comment.getCommentId() = 123696314773704705
         * comment.getCommentId() = 123696314740150275
         */
    }

    @Getter
    @AllArgsConstructor
    public static class CommentCreateRequest {
        private Long jobpostingId;
        private String content;
        private Long parentCommentId;
        private Long userId;
    }
}