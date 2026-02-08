package halo.corebridge.jobpostinghot.batch;

import halo.corebridge.jobpostinghot.client.CommentClient;
import halo.corebridge.jobpostinghot.client.JobpostingClient;
import halo.corebridge.jobpostinghot.client.LikeClient;
import halo.corebridge.jobpostinghot.client.ViewClient;
import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import halo.corebridge.jobpostinghot.model.entity.HotJobpostingId;
import halo.corebridge.jobpostinghot.repository.HotJobpostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HotScoreJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final HotJobpostingRepository hotJobpostingRepository;
    private final JobpostingClient jobpostingClient;
    private final ViewClient viewClient;
    private final LikeClient likeClient;
    private final CommentClient commentClient;

    private static final int CHUNK_SIZE = 50;

    // ============================================
    // Job 정의
    // ============================================

    @Bean
    public Job hotScoreJob() {
        return new JobBuilder("hotScoreJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fetchAndScoreStep())
                .listener(new HotScoreJobListener())
                .build();
    }

    // ============================================
    // Step 정의: 채용공고 조회 → 점수 계산 → 저장
    // ============================================

    @Bean
    public Step fetchAndScoreStep() {
        return new StepBuilder("fetchAndScoreStep", jobRepository)
                .<JobpostingClient.JobpostingResponse, HotJobposting>chunk(CHUNK_SIZE, transactionManager)
                .reader(jobpostingItemReader())
                .processor(hotScoreProcessor())
                .writer(hotJobpostingItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .retry(Exception.class)
                .retryLimit(3)
                .build();
    }

    // ============================================
    // Reader: 전체 채용공고를 페이징으로 읽기
    // ============================================

    @Bean
    public ItemReader<JobpostingClient.JobpostingResponse> jobpostingItemReader() {
        return new ItemReader<>() {
            private Iterator<JobpostingClient.JobpostingResponse> currentIterator;
            private long currentBoardId = 1;
            private long currentPage = 1;
            private boolean finished = false;

            @Override
            public JobpostingClient.JobpostingResponse read() {
                if (finished) {
                    return null;
                }

                while (currentBoardId <= 5) {
                    // 현재 iterator에 남은 항목이 있으면 반환
                    if (currentIterator != null && currentIterator.hasNext()) {
                        return currentIterator.next();
                    }

                    // 다음 페이지 조회
                    JobpostingClient.JobpostingPageResponse page =
                            jobpostingClient.readAll(currentBoardId, currentPage, (long) CHUNK_SIZE);

                    if (page != null && page.getJobpostings() != null && !page.getJobpostings().isEmpty()) {
                        currentIterator = page.getJobpostings().iterator();
                        currentPage++;
                        continue;
                    }

                    // 이 게시판은 끝 → 다음 게시판으로
                    currentBoardId++;
                    currentPage = 1;
                    currentIterator = null;
                }

                finished = true;
                return null;
            }
        };
    }

    // ============================================
    // Processor: 통계 조회 + 점수 계산
    // ============================================

    @Bean
    public ItemProcessor<JobpostingClient.JobpostingResponse, HotJobposting> hotScoreProcessor() {
        return jobposting -> {
            Long jobpostingId = jobposting.getJobpostingId();
            LocalDate today = LocalDate.now();

            // 각 서비스에서 실시간 통계 조회
            Long viewCount = viewClient.count(jobpostingId);
            Long likeCount = likeClient.count(jobpostingId);
            Long commentCount = commentClient.count(jobpostingId);

            // 기존 데이터가 있으면 업데이트, 없으면 생성
            HotJobpostingId id = new HotJobpostingId(today, jobpostingId);
            return hotJobpostingRepository.findById(id)
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
        };
    }

    // ============================================
    // Writer: 청크 단위로 DB에 저장
    // ============================================

    @Bean
    public ItemWriter<HotJobposting> hotJobpostingItemWriter() {
        return items -> {
            hotJobpostingRepository.saveAll(items.getItems());
            log.info("[Batch Writer] Saved {} hot jobpostings", items.size());
        };
    }
}
