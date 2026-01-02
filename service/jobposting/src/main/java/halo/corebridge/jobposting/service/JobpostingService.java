package halo.corebridge.jobposting.service;

import halo.corebridge.infra.id.snowflake.Snowflake;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.model.entity.Jobposting;
import halo.corebridge.jobposting.repository.JobpostingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobpostingService {
    private final Snowflake snowflake = new Snowflake();
    private final JobpostingRepository jobpostingRepository;

    @Transactional
    public JobpostingDto.JobpostingResponse create(JobpostingDto.JobpostingCreateRequest request) {
        Jobposting jobposting = jobpostingRepository.save(
          Jobposting.create(snowflake.nextId(), request.getTitle(), request.getContent(), request.getBoardId(), request.getUserId())
        );

        return JobpostingDto.JobpostingResponse.from(jobposting);
    }

    @Transactional
    public JobpostingDto.JobpostingResponse update(Long jobpostingId, JobpostingDto.JobpostingUpdateRequest request) {
        Jobposting jobposting = jobpostingRepository.findById(jobpostingId).orElseThrow();
        jobposting.update(request.getTitle(), request.getContent());

        return JobpostingDto.JobpostingResponse.from(jobposting);
    }

    @Transactional
    public JobpostingDto.JobpostingResponse read(Long jobpostingId) {
        return JobpostingDto.JobpostingResponse.from(jobpostingRepository.findById(jobpostingId).orElseThrow());
    }

    @Transactional
    public void delete(Long jobpostingId) {
        jobpostingRepository.deleteById(jobpostingId);
    }


    public JobpostingDto.JobpostingPageResponse readAll(Long boardId, Long page, Long pageSize) {
        return JobpostingDto.JobpostingPageResponse.of(
                jobpostingRepository.findAll(boardId, (page - 1) * pageSize, pageSize).stream()
                        .map(JobpostingDto.JobpostingResponse::from)
                        .toList(),
                jobpostingRepository.count(
                        boardId,
                        PageLimitCalculator.calculatePageLimit(page, pageSize, 10L)
                )
        );
    }

}
