package halo.corebridge.jobpostingread.service;

import halo.corebridge.jobpostingread.client.*;
import halo.corebridge.jobpostingread.model.dto.JobpostingReadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobpostingReadService {
    
    private final JobpostingClient jobpostingClient;
    private final ViewClient viewClient;
    private final LikeClient likeClient;
    private final CommentClient commentClient;
    private final UserClient userClient;

    /**
     * 단일 채용공고 조회 (통계 포함)
     */
    public JobpostingReadDto.Response read(Long jobpostingId) {
        JobpostingClient.JobpostingResponse jobposting = jobpostingClient.read(jobpostingId);
        
        if (jobposting == null) {
            throw new RuntimeException("Jobposting not found: " + jobpostingId);
        }
        
        return toReadResponse(jobposting);
    }

    /**
     * 채용공고 목록 조회 (통계 포함)
     */
    public JobpostingReadDto.PageResponse readAll(Long boardId, Long page, Long pageSize) {
        JobpostingClient.JobpostingPageResponse pageResponse = jobpostingClient.readAll(boardId, page, pageSize);
        
        if (pageResponse == null || pageResponse.getJobpostings() == null) {
            return JobpostingReadDto.PageResponse.of(List.of(), 0L);
        }

        List<JobpostingReadDto.Response> jobpostings = pageResponse.getJobpostings()
                .stream()
                .map(this::toReadResponse)
                .toList();

        return JobpostingReadDto.PageResponse.of(jobpostings, pageResponse.getJobpostingCount());
    }

    /**
     * Jobposting -> ReadResponse 변환 (각 서비스에서 통계 조회)
     */
    private JobpostingReadDto.Response toReadResponse(JobpostingClient.JobpostingResponse jobposting) {
        Long jobpostingId = jobposting.getJobpostingId();
        
        return JobpostingReadDto.Response.builder()
                .jobpostingId(jobpostingId)
                .title(jobposting.getTitle())
                .content(jobposting.getContent())
                .boardId(jobposting.getBoardId())
                .userId(jobposting.getUserId())
                .nickname(userClient.getNickname(jobposting.getUserId()))
                .viewCount(viewClient.count(jobpostingId))
                .likeCount(likeClient.count(jobpostingId))
                .commentCount(commentClient.count(jobpostingId))
                .createdAt(jobposting.getCreatedAt())
                .updatedAt(jobposting.getUpdatedAt())
                .build();
    }
}
