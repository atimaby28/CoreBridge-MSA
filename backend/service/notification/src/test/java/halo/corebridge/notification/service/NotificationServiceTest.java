package halo.corebridge.notification.service;

import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.entity.Notification;
import halo.corebridge.notification.model.enums.NotificationType;
import halo.corebridge.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Long userId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        userId = 1L;
        testNotification = Notification.builder()
                .id(1L)
                .userId(userId)
                .type(NotificationType.DOCUMENT_PASS)
                .title("서류 합격")
                .message("축하합니다! 서류 전형에 합격하셨습니다.")
                .link("/applies/123")
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("알림 생성 성공")
    void createNotification_Success() {
        // given
        NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                .userId(userId)
                .type(NotificationType.DOCUMENT_PASS)
                .title("서류 합격")
                .message("축하합니다!")
                .build();

        given(notificationRepository.save(any(Notification.class))).willReturn(testNotification);

        // when
        NotificationDto.CreateResponse response = notificationService.create(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("내 알림 목록 조회")
    void getMyNotifications() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> notificationPage = new PageImpl<>(List.of(testNotification));
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(notificationPage);

        // when
        Page<NotificationDto.Response> result = notificationService.getMyNotifications(userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationType.DOCUMENT_PASS);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회")
    void getUnreadCount() {
        // given
        given(notificationRepository.countByUserIdAndIsReadFalse(userId)).willReturn(5L);

        // when
        NotificationDto.UnreadCountResponse response = notificationService.getUnreadCount(userId);

        // then
        assertThat(response.getCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() {
        // given
        given(notificationRepository.findById(1L)).willReturn(Optional.of(testNotification));

        // when
        boolean result = notificationService.markAsRead(userId, 1L);

        // then
        assertThat(result).isTrue();
        assertThat(testNotification.isRead()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 알림 읽음 처리 실패")
    void markAsRead_WrongUser_Fail() {
        // given
        given(notificationRepository.findById(1L)).willReturn(Optional.of(testNotification));

        // when
        boolean result = notificationService.markAsRead(999L, 1L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead() {
        // given
        given(notificationRepository.markAllAsReadByUserId(userId)).willReturn(3);

        // when
        int count = notificationService.markAllAsRead(userId);

        // then
        assertThat(count).isEqualTo(3);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }
}
