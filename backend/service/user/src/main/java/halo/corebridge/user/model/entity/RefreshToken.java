package halo.corebridge.user.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // === 정적 팩토리 메서드 ===

    public static RefreshToken create(String token, Long userId, long expirationMs) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = token;
        refreshToken.userId = userId;
        refreshToken.createdAt = LocalDateTime.now();
        refreshToken.expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
        return refreshToken;
    }

    // === 비즈니스 메서드 ===

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void updateToken(String newToken, long expirationMs) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
    }
}
