package halo.corebridge.jobpostingview.service;

import halo.corebridge.jobpostingview.entity.JobpostingViewCount;
import halo.corebridge.jobpostingview.repository.JobpostingViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobpostingViewService {
    
    private final JobpostingViewCountRepository jobpostingViewCountRepository;

    /**
     * 조회수 증가
     * - 같은 사용자가 여러번 조회해도 모두 카운트 (단순 버전)
     * - 실제로는 Redis + 분산락으로 중복 방지 필요
     */
    @Transactional
    public Long increase(Long jobpostingId, Long userId) {
        int updated = jobpostingViewCountRepository.increase(jobpostingId);
        
        if (updated == 0) {
            // 최초 조회 시 레코드가 없으면 생성
            jobpostingViewCountRepository.save(
                JobpostingViewCount.init(jobpostingId, 1L)
            );
            return 1L;
        }
        
        return count(jobpostingId);
    }

    /**
     * 조회수 조회
     */
    @Transactional(readOnly = true)
    public Long count(Long jobpostingId) {
        return jobpostingViewCountRepository.findById(jobpostingId)
                .map(JobpostingViewCount::getViewCount)
                .orElse(0L);
    }
}
