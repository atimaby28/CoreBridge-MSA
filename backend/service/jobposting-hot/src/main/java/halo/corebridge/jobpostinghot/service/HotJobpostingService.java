package halo.corebridge.jobpostinghot.service;

import halo.corebridge.jobpostinghot.client.CommentClient;
import halo.corebridge.jobpostinghot.client.JobpostingClient;
import halo.corebridge.jobpostinghot.client.LikeClient;
import halo.corebridge.jobpostinghot.client.ViewClient;
import halo.corebridge.jobpostinghot.model.dto.HotJobpostingDto;
import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import halo.corebridge.jobpostinghot.model.entity.HotJobpostingId;
import halo.corebridge.jobpostinghot.repository.HotJobpostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotJobpostingService {
    
    private final HotJobpostingRepository hotJobpostingRepository;
    private final JobpostingClient jobpostingClient;
    private final ViewClient viewClient;
    private final LikeClient likeClient;
    private final CommentClient commentClient;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ============================================
    // 조회
    // ============================================

    /**
     * 특정 날짜의 인기 공고 목록 조회
     */
    @Transactional(readOnly = true)
    public List<HotJobpostingDto.Response> readAll(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        
        return hotJobpostingRepository.findByDateKeyOrderByScoreDesc(date)
                .stream()
                .map(HotJobpostingDto.Response::from)
                .toList();
    }

    /**
     * 오늘의 인기 공고 TOP N
     */
    @Transactional(readOnly = true)
    public List<HotJobpostingDto.Response> readTopN(int limit) {
        LocalDate today = LocalDate.now();
        
        return hotJobpostingRepository.findTopByDateKey(today, limit)
                .stream()
                .map(HotJobpostingDto.Response::from)
                .toList();
    }

    // ============================================
    // 수동 등록/갱신
    // ============================================

    /**
     * 단일 채용공고를 인기 공고로 등록/갱신
     */
    @Transactional
    public HotJobpostingDto.Response register(Long jobpostingId) {
        JobpostingClient.JobpostingResponse jobposting = jobpostingClient.read(jobpostingId);
        
        if (jobposting == null) {
            throw new RuntimeException("Jobposting not found: " + jobpostingId);
        }

        LocalDate today = LocalDate.now();
        
        // 각 서비스에서 통계 조회
        Long viewCount = viewClient.count(jobpostingId);
        Long likeCount = likeClient.count(jobpostingId);
        Long commentCount = commentClient.count(jobpostingId);

        // 기존 데이터 확인
        HotJobpostingId id = new HotJobpostingId(today, jobpostingId);
        HotJobposting hotJobposting = hotJobpostingRepository.findById(id)
                .map(existing -> {
                    existing.updateCounts(likeCount, commentCount, viewCount);
                    return existing;
                })
                .orElseGet(() -> HotJobposting.create(
                        today,
                        jobpostingId,
                        jobposting.getTitle(),
                        jobposting.getBoardId(),
                        likeCount,
                        commentCount,
                        viewCount
                ));

        hotJobpostingRepository.save(hotJobposting);
        
        log.info("Registered hot jobposting: id={}, score={}", jobpostingId, hotJobposting.getScore());
        
        return HotJobpostingDto.Response.from(hotJobposting);
    }

    /**
     * 특정 게시판의 모든 채용공고를 인기 공고로 갱신
     */
    @Transactional
    public int updateByBoard(Long boardId) {
        JobpostingClient.JobpostingPageResponse pageResponse = 
                jobpostingClient.readAll(boardId, 1L, 100L);
        
        if (pageResponse == null || pageResponse.getJobpostings() == null) {
            return 0;
        }

        int count = 0;
        for (JobpostingClient.JobpostingResponse jobposting : pageResponse.getJobpostings()) {
            try {
                register(jobposting.getJobpostingId());
                count++;
            } catch (Exception e) {
                log.error("Failed to register hot jobposting: {}", jobposting.getJobpostingId(), e);
            }
        }
        
        log.info("Updated {} hot jobpostings for board {}", count, boardId);
        return count;
    }

    /**
     * 전체 채용공고 인기 점수 갱신 (스케줄러에서 호출)
     */
    @Transactional
    public int updateAll() {
        int totalCount = 0;
        
        // 게시판 1~5 순회 (전체, IT/개발, 마케팅, 디자인, 영업)
        for (long boardId = 1; boardId <= 5; boardId++) {
            totalCount += updateByBoard(boardId);
        }
        
        log.info("Total {} hot jobpostings updated", totalCount);
        return totalCount;
    }

    /**
     * 인기 공고 목록에 실시간 통계 반영
     */
    @Transactional(readOnly = true)
    public List<HotJobpostingDto.Response> readTopNWithLiveStats(int limit) {
        LocalDate today = LocalDate.now();
        
        List<HotJobposting> hotList = hotJobpostingRepository.findTopByDateKey(today, limit);
        
        return hotList.stream()
                .map(hot -> {
                    // 실시간 통계 조회
                    Long liveViewCount = viewClient.count(hot.getJobpostingId());
                    Long liveLikeCount = likeClient.count(hot.getJobpostingId());
                    Long liveCommentCount = commentClient.count(hot.getJobpostingId());
                    
                    return HotJobpostingDto.Response.builder()
                            .jobpostingId(hot.getJobpostingId())
                            .title(hot.getTitle())
                            .boardId(hot.getBoardId())
                            .likeCount(liveLikeCount)
                            .commentCount(liveCommentCount)
                            .viewCount(liveViewCount)
                            .score(hot.getScore())
                            .build();
                })
                .toList();
    }
}
