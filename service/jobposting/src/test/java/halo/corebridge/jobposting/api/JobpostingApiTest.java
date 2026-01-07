package halo.corebridge.jobposting.api;

import halo.corebridge.common.web.ApiPaths;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class JobpostingApiTest {

    RestClient restClient = RestClient.create(ApiPaths.HOST + "8002");

    @Test
    void createTest() {
        JobpostingDto.JobpostingResponse response = create(new JobpostingCreateRequest(
                "First title", "Hi, This is my first content", 1L, 1L
        ));

        System.out.println("response = " + response);
    }

    JobpostingDto.JobpostingResponse create(JobpostingCreateRequest request) {
        return restClient.post()
                .uri(ApiPaths.JOB_POSTINGS)
                .body(request)
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void readTest() {
        JobpostingDto.JobpostingResponse response = read(2370338985115648L);
        System.out.println("response = " + response);
    }

    JobpostingDto.JobpostingResponse read(Long jobpostingId) {
        return restClient.get()
                .uri(ApiPaths.JOB_POSTINGS + "/{jobpostingId}", jobpostingId)
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void updateTest() {
        update(2370338985115648L);
        JobpostingDto.JobpostingResponse response = read(2370338985115648L);
        System.out.println("response = " + response);
    }

    void update(Long jobpostingId) {
        restClient.put()
                .uri(ApiPaths.JOB_POSTINGS + "/{jobpostingId}", jobpostingId)
                .body(new JobpostingUpdateRequest("Second title", "Hi, This is my second content"))
                .retrieve()
                .body(JobpostingDto.JobpostingResponse.class);
    }

    @Test
    void deleteTest() {
        restClient.delete()
                .uri(ApiPaths.JOB_POSTINGS + "/{jobpostingId}", 2370338985115648L)
                .retrieve()
                .toBodilessEntity();
    }

    @Test
    void readAllTest() {
        JobpostingDto.JobpostingPageResponse response = restClient.get()
                .uri(ApiPaths.JOB_POSTINGS + "?boardId=1&pageSize=30&page=1")
                .retrieve()
                .body(JobpostingDto.JobpostingPageResponse.class);

        System.out.println("response.getJobpostingCount() = " + response.getJobpostingCount());

        for (JobpostingDto.JobpostingResponse jobposting : response.getJobpostings()) {
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
}
