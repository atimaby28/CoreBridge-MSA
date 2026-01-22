package halo.corebridge.jobposting.api;

import halo.corebridge.jobposting.model.dto.JobpostingDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

public class JobpostingApiTest {

    RestClient restClient = RestClient.create("http://localhost:8002");

    @Test
    void createTest() {
        JobpostingDto.JobpostingResponse response = create(new JobpostingCreateRequest(
                "First title", "Hi, This is my first content", 1L, 1L
        ));

        System.out.println("response = " + response);
    }

    JobpostingDto.JobpostingResponse create(JobpostingCreateRequest request) {
        return restClient.post()
                .uri("/api/v1/jobpostings")
                .body(request)
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void readTest() {
        JobpostingDto.JobpostingResponse response = read(7706939081699328L);
        System.out.println("response = " + response);
    }

    JobpostingDto.JobpostingResponse read(Long jobpostingId) {
        return restClient.get()
                .uri("/api/v1/jobpostings/{jobpostingId}", jobpostingId)
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void updateTest() {
        update(7706939081699328L);
        JobpostingDto.JobpostingResponse response = read(7706939081699328L);
        System.out.println("response = " + response);
    }

    void update(Long jobpostingId) {
        restClient.put()
                .uri("/api/v1/jobpostings/{jobpostingId}", jobpostingId)
                .body(new JobpostingUpdateRequest("Second title", "Hi, This is my second content"))
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void deleteTest() {
        restClient.delete()
                .uri("/api/v1/jobpostings/{jobpostingId}", 7703216796282880L)
                .retrieve()
                .toBodilessEntity();
    }

    @Test
    void readAllTest() {
        ApiResponse<JobpostingDto.JobpostingPageResponse> response = restClient.get()
                .uri("/api/v1/jobpostings?boardId=1&pageSize=30&page=1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        System.out.println("response.getJobpostingCount() = " + response.getResult().getJobpostingCount());

        for (JobpostingDto.JobpostingResponse jobposting : response.getResult().getJobpostings()) {
            System.out.println("jobposting.getJobpostingId() = " + jobposting.getJobpostingId());
        }
    }

    @Getter
    @AllArgsConstructor
    static class JobpostingCreateRequest {
        private String title;
        private String content;
        private Long userId;
        private Long boardId;
    }

    @Getter
    @AllArgsConstructor
    static class JobpostingUpdateRequest {
        private String title;
        private String content;
    }

    // wrapper 클래스 추가
    @Getter
    @NoArgsConstructor
    static class ApiResponse<T> {
        private boolean isSuccess;
        private int code;
        private String message;
        private T result;
    }
}
