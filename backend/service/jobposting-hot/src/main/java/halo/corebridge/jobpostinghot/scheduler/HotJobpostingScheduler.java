package halo.corebridge.jobpostinghot.scheduler;

import halo.corebridge.jobpostinghot.service.HotJobpostingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotJobpostingScheduler {
    
    private final HotJobpostingService hotJobpostingService;

    /**
     * 서비스 시작 시 인기 공고 갱신
     */
    @PostConstruct
    public void initOnStartup() {
        log.info("=== Starting initial hot jobposting update on startup ===");
        try {
            int count = hotJobpostingService.updateAll();
            log.info("=== Completed initial update: {} jobpostings ===", count);
        } catch (Exception e) {
            log.warn("=== Initial hot jobposting update failed (services may not be ready): {} ===", e.getMessage());
        }
    }

    /**
     * 매시간 인기 공고 갱신
     * cron: 초 분 시 일 월 요일
     * "0 0 * * * *" = 매시간 정각
     */
    @Scheduled(cron = "0 0 * * * *")
    public void updateHotJobpostingsHourly() {
        log.info("=== Starting hourly hot jobposting update ===");
        try {
            int count = hotJobpostingService.updateAll();
            log.info("=== Completed hourly update: {} jobpostings ===", count);
        } catch (Exception e) {
            log.error("=== Failed hourly hot jobposting update ===", e);
        }
    }

    /**
     * 매일 자정 인기 공고 갱신 (새로운 날짜 데이터 생성)
     * "0 0 0 * * *" = 매일 자정
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateHotJobpostingsDaily() {
        log.info("=== Starting daily hot jobposting update ===");
        try {
            int count = hotJobpostingService.updateAll();
            log.info("=== Completed daily update: {} jobpostings ===", count);
        } catch (Exception e) {
            log.error("=== Failed daily hot jobposting update ===", e);
        }
    }
}
