package halo.corebridge.user.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    private String nickname;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private boolean enabled;

    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    // === 정적 팩토리 메서드 ===

    public static User create(Long userId, String email, String nickname,
                              String encodedPassword, UserRole role) {
        User user = new User();
        user.userId = userId;
        user.email = email;
        user.nickname = nickname;
        user.password = encodedPassword;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        user.enabled = true;
        return user;
    }

    // === 비즈니스 메서드 ===

    public void updateProfile(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
        this.enabled = (status == UserStatus.ACTIVE);
    }

    public void markDeleted() {
        this.status = UserStatus.DELETED;
        this.enabled = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
