package halo.corebridge.jobposting.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Jobposting extends BaseTimeEntity {
    @Id
    private Long jobpostingId;
    private String title;
    private String content;
    private Long boardId; // shard key
    private Long userId;

    public static Jobposting create(Long jobpostingId, String title, String content, Long boardId, Long userId) {
        Jobposting jobposting = new Jobposting();
        jobposting.jobpostingId = jobpostingId;
        jobposting.title = title;
        jobposting.content = content;
        jobposting.boardId = boardId;
        jobposting.userId = userId;

        jobposting.createdAt = LocalDateTime.now();
        jobposting.updatedAt = jobposting.createdAt;

        return jobposting;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        updatedAt = LocalDateTime.now();
    }
}
