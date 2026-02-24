package halo.corebridge.jobposting.repository;

import halo.corebridge.jobposting.model.entity.Jobposting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("JobpostingRepository 테스트")
class JobpostingRepositoryTest {

    @Autowired
    private JobpostingRepository jobpostingRepository;

    @BeforeEach
    void setUp() {
        jobpostingRepository.deleteAll();
        for (int i = 1; i <= 5; i++) {
            jobpostingRepository.save(
                    Jobposting.create((long) i, "공고 제목 " + i, "공고 내용 " + i, 1L, 100L)
            );
        }
    }

    @Test
    @DisplayName("성공: boardId별 페이징 조회")
    void findAll_byBoardId() {
        List<Jobposting> result = jobpostingRepository.findAll(1L, 0L, 3L);
        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("성공: 전체 공고 개수 조회")
    void count_byBoardId() {
        Long count = jobpostingRepository.count(1L, 100L);
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("성공: 작성자별 공고 목록 조회")
    void findByUserId() {
        List<Jobposting> result = jobpostingRepository.findByUserIdOrderByCreatedAtDesc(100L);
        assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("성공: 존재하지 않는 boardId 조회 시 빈 목록")
    void findAll_emptyBoard() {
        List<Jobposting> result = jobpostingRepository.findAll(999L, 0L, 10L);
        assertThat(result).isEmpty();
    }
}
