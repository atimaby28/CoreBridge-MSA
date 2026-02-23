package halo.corebridge.jobposting.repository;

import halo.corebridge.jobposting.model.entity.Jobposting;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class JobpostingRepositoryTest {
    @Autowired
    JobpostingRepository jobpostingRepository;

    @Test
    void findAllTest() {
        List<Jobposting> jobpostings = jobpostingRepository.findAll(1L, 1_499_970L, 30L);

        log.info("jobpostings.size() = {}", jobpostings.size());

        for (Jobposting jobposting : jobpostings) {
            log.info("jobpostings= {}", jobposting);
        }
    }

    @Test
    void countTest() {
        Long count = jobpostingRepository.count(1L, 10_000L);
        log.info("count = {}", count);
    }
}