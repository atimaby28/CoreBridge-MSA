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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Snowflake snowflake;

    // 기본 Admin 계정 정보
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "qwer1234";
    private static final String ADMIN_NICKNAME = "관리자";

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin 계정이 이미 존재합니다: {}", ADMIN_EMAIL);
            return;
        }

        User admin = User.create(
                snowflake.nextId(),
                ADMIN_EMAIL,
                ADMIN_NICKNAME,
                passwordEncoder.encode(ADMIN_PASSWORD),
                UserRole.ROLE_ADMIN
        );

        userRepository.save(admin);
        log.info("기본 Admin 계정 생성 완료: {}", ADMIN_EMAIL);
    }
}
