package halo.corebridge.notification.repository;

import halo.corebridge.notification.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 모든 알림 조회 (최신순)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 조회 (최신순)
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 최근 N개 알림 조회
     */
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 관련 데이터의 알림 조회
     */
    List<Notification> findByRelatedIdAndRelatedType(Long relatedId, String relatedType);
}
