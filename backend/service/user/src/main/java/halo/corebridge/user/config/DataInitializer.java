package halo.corebridge.user.config;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.user.model.entity.User;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 기본 계정을 생성하는 초기화 클래스.
 * - Admin 계정 (관리자)
 * - Company 계정 (기업 채용담당자)
 * - User 계정 (일반 지원자)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Snowflake snowflake;

    private static final String DEFAULT_PASSWORD = "qwer1234";

    // 고정 ID — Jobposting/Resume DataInitializer와 공유 (Snowflake 범위)
    private static final long ADMIN_ID   = 11234023833772033L;
    private static final long COMPANY_ID = 11234023833772034L;
    private static final long USER_ID   = 11234028028076038L;

    @Override
    public void run(ApplicationArguments args) {
        // Admin
        createUserIfNotExists(ADMIN_ID, "admin@test.com", "관리자", UserRole.ROLE_ADMIN);

        // Company (기업)
        createUserIfNotExists(COMPANY_ID, "company@test.com", "테크컴퍼니", UserRole.ROLE_COMPANY);

        // User (지원자)
        createUserIfNotExists(USER_ID, "user@test.com", "양승우", UserRole.ROLE_USER);

        log.info("=== DataInitializer 완료 ===");
    }

    private void createUserIfNotExists(Long userId, String email, String nickname, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            log.info("계정이 이미 존재합니다: {} ({})", email, role);
            return;
        }

        User user = User.create(
                userId,
                email,
                nickname,
                passwordEncoder.encode(DEFAULT_PASSWORD),
                role
        );

        userRepository.save(user);
        log.info("계정 생성 완료: {} / {} / ID={} ({})", email, nickname, userId, role);
    }
}
