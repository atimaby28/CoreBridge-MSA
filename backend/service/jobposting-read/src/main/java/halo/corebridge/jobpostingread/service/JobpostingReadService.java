package halo.corebridge.jobpostingread.service;

import halo.corebridge.jobpostingread.client.*;
import halo.corebridge.jobpostingread.handler.JobpostingReadCache;
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
    private final JobpostingReadCache readCache;

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
     * Jobposting -> ReadResponse 변환
     * 이벤트 캐시에 데이터가 있으면 우선 사용, 없으면 HTTP fallback
     */
    private JobpostingReadDto.Response toReadResponse(JobpostingClient.JobpostingResponse jobposting) {
        Long jobpostingId = jobposting.getJobpostingId();

        // 캐시 우선 조회, 없으면 HTTP fallback
        Long viewCount = readCache.getViewCount(jobpostingId);
        if (viewCount == null) viewCount = viewClient.count(jobpostingId);

        Long likeCount = readCache.getLikeCount(jobpostingId);
        if (likeCount == null) likeCount = likeClient.count(jobpostingId);

        Long commentCount = readCache.getCommentCount(jobpostingId);
        if (commentCount == null) commentCount = commentClient.count(jobpostingId);

        return JobpostingReadDto.Response.builder()
                .jobpostingId(jobpostingId)
                .title(jobposting.getTitle())
                .content(jobposting.getContent())
                .boardId(jobposting.getBoardId())
                .userId(jobposting.getUserId())
                .nickname(userClient.getNickname(jobposting.getUserId()))
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .createdAt(jobposting.getCreatedAt())
                .updatedAt(jobposting.getUpdatedAt())
                .build();
    }
}
