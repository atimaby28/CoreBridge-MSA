package halo.corebridge.jobpostingread.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 이벤트 기반으로 수신한 통계 데이터를 로컬 캐시에 보관.
 * CQRS 읽기 모델의 역할 — HTTP 호출 없이 즉시 응답 가능.
 */
@Slf4j
@Component
public class JobpostingReadCache {

    // jobpostingId -> viewCount
    private final Map<Long, AtomicLong> viewCountCache = new ConcurrentHashMap<>();

    // jobpostingId -> likeCount
    private final Map<Long, AtomicLong> likeCountCache = new ConcurrentHashMap<>();

    // jobpostingId -> commentCount
    private final Map<Long, AtomicLong> commentCountCache = new ConcurrentHashMap<>();

    public void updateViewCount(Long jobpostingId, Long viewCount) {
        viewCountCache.computeIfAbsent(jobpostingId, k -> new AtomicLong(0)).set(viewCount);
        log.debug("[ReadCache] viewCount updated: jobpostingId={}, count={}", jobpostingId, viewCount);
    }

    public void updateLikeCount(Long jobpostingId, Long likeCount) {
        likeCountCache.computeIfAbsent(jobpostingId, k -> new AtomicLong(0)).set(likeCount);
        log.debug("[ReadCache] likeCount updated: jobpostingId={}, count={}", jobpostingId, likeCount);
    }

    public void incrementCommentCount(Long jobpostingId) {
        commentCountCache.computeIfAbsent(jobpostingId, k -> new AtomicLong(0)).incrementAndGet();
        log.debug("[ReadCache] commentCount incremented: jobpostingId={}", jobpostingId);
    }

    public void decrementCommentCount(Long jobpostingId) {
        commentCountCache.computeIfAbsent(jobpostingId, k -> new AtomicLong(0))
                .updateAndGet(v -> Math.max(0, v - 1));
        log.debug("[ReadCache] commentCount decremented: jobpostingId={}", jobpostingId);
    }

    public void removeJobposting(Long jobpostingId) {
        viewCountCache.remove(jobpostingId);
        likeCountCache.remove(jobpostingId);
        commentCountCache.remove(jobpostingId);
        log.debug("[ReadCache] removed: jobpostingId={}", jobpostingId);
    }

    public Long getViewCount(Long jobpostingId) {
        AtomicLong count = viewCountCache.get(jobpostingId);
        return count != null ? count.get() : null;
    }

    public Long getLikeCount(Long jobpostingId) {
        AtomicLong count = likeCountCache.get(jobpostingId);
        return count != null ? count.get() : null;
    }

    public Long getCommentCount(Long jobpostingId) {
        AtomicLong count = commentCountCache.get(jobpostingId);
        return count != null ? count.get() : null;
    }
}
