package halo.corebridge.user.model.entity;

import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Nested
    @DisplayName("User.create() 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("User 생성 시 기본값이 올바르게 설정된다")
        void createUser_WithValidInput_SetsDefaultValues() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String nickname = "테스터";
            String encodedPassword = "encodedPassword123";
            UserRole role = UserRole.ROLE_USER;

            // when
            User user = User.create(userId, email, nickname, encodedPassword, role);

            // then
            assertThat(user.getUserId()).isEqualTo(userId);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getNickname()).isEqualTo(nickname);
            assertThat(user.getPassword()).isEqualTo(encodedPassword);
            assertThat(user.getRole()).isEqualTo(role);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getLastLoginAt()).isNull();
            assertThat(user.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("ROLE_COMPANY로 User를 생성할 수 있다")
        void createUser_WithCompanyRole_Success() {
            // given
            Long userId = 2L;
            String email = "company@example.com";
            String nickname = "기업회원";
            String encodedPassword = "encodedPassword123";
            UserRole role = UserRole.ROLE_COMPANY;

            // when
            User user = User.create(userId, email, nickname, encodedPassword, role);

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ROLE_COMPANY);
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("ROLE_ADMIN으로 User를 생성할 수 있다")
        void createUser_WithAdminRole_Success() {
            // given
            Long userId = 3L;
            String email = "admin@example.com";
            String nickname = "관리자";
            String encodedPassword = "encodedPassword123";
            UserRole role = UserRole.ROLE_ADMIN;

            // when
            User user = User.create(userId, email, nickname, encodedPassword, role);

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        }
    }

    @Nested
    @DisplayName("User 비즈니스 메서드")
    class BusinessMethodTest {

        @Test
        @DisplayName("updateProfile()로 닉네임을 변경할 수 있다")
        void updateProfile_ChangesNickname() {
            // given
            User user = createTestUser();
            String newNickname = "새닉네임";

            // when
            user.updateProfile(newNickname);

            // then
            assertThat(user.getNickname()).isEqualTo(newNickname);
        }

        @Test
        @DisplayName("updatePassword()로 비밀번호를 변경할 수 있다")
        void updatePassword_ChangesPassword() {
            // given
            User user = createTestUser();
            String newPassword = "newEncodedPassword456";

            // when
            user.updatePassword(newPassword);

            // then
            assertThat(user.getPassword()).isEqualTo(newPassword);
        }

        @Test
        @DisplayName("changeRole()로 역할을 변경할 수 있다")
        void changeRole_ChangesUserRole() {
            // given
            User user = createTestUser();

            // when
            user.changeRole(UserRole.ROLE_COMPANY);

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ROLE_COMPANY);
        }

        @Test
        @DisplayName("changeStatus()로 BLOCKED 상태로 변경하면 enabled가 false가 된다")
        void changeStatus_ToBlocked_DisablesUser() {
            // given
            User user = createTestUser();
            assertThat(user.isEnabled()).isTrue();

            // when
            user.changeStatus(UserStatus.BLOCKED);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("changeStatus()로 ACTIVE 상태로 변경하면 enabled가 true가 된다")
        void changeStatus_ToActive_EnablesUser() {
            // given
            User user = createTestUser();
            user.changeStatus(UserStatus.BLOCKED);
            assertThat(user.isEnabled()).isFalse();

            // when
            user.changeStatus(UserStatus.ACTIVE);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("markDeleted()로 사용자를 삭제 상태로 변경할 수 있다")
        void markDeleted_SetsDeletedStatus() {
            // given
            User user = createTestUser();

            // when
            user.markDeleted();

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("recordLogin()으로 마지막 로그인 시간을 기록할 수 있다")
        void recordLogin_SetsLastLoginAt() {
            // given
            User user = createTestUser();
            assertThat(user.getLastLoginAt()).isNull();

            // when
            user.recordLogin();

            // then
            assertThat(user.getLastLoginAt()).isNotNull();
        }
    }

    private User createTestUser() {
        return User.create(
                1L,
                "test@example.com",
                "테스터",
                "encodedPassword123",
                UserRole.ROLE_USER
        );
    }
}
