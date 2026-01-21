package halo.corebridge.user.service;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.user.exception.UserNotFoundException;
import halo.corebridge.user.model.dto.UserDto;
import halo.corebridge.user.model.entity.RefreshToken;
import halo.corebridge.user.model.entity.User;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import halo.corebridge.user.repository.RefreshTokenRepository;
import halo.corebridge.user.repository.UserRepository;
import halo.corebridge.user.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private Snowflake snowflake;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create(1L, "test@example.com", "테스터", "encodedPassword", UserRole.ROLE_USER);
    }

    @Nested
    @DisplayName("signup() - 회원가입")
    class SignupTest {

        @Test
        @DisplayName("정상적인 회원가입이 성공한다")
        void signup_WithValidRequest_Success() {
            // given
            UserDto.SignupRequest request = createSignupRequest("newuser@example.com", "신규유저", "password123", null);

            given(userRepository.existsByEmail("newuser@example.com")).willReturn(false);
            given(snowflake.nextId()).willReturn(100L);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            UserDto.UserResponse response = userService.signup(request);

            // then
            assertThat(response.getEmail()).isEqualTo("newuser@example.com");
            assertThat(response.getNickname()).isEqualTo("신규유저");
            assertThat(response.getRole()).isEqualTo(UserRole.ROLE_USER);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("ROLE_COMPANY로 회원가입이 가능하다")
        void signup_WithCompanyRole_Success() {
            // given
            UserDto.SignupRequest request = createSignupRequest("company@example.com", "기업회원", "password123", UserRole.ROLE_COMPANY);

            given(userRepository.existsByEmail("company@example.com")).willReturn(false);
            given(snowflake.nextId()).willReturn(101L);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            UserDto.UserResponse response = userService.signup(request);

            // then
            assertThat(response.getRole()).isEqualTo(UserRole.ROLE_COMPANY);
        }

        @Test
        @DisplayName("ROLE_ADMIN으로 회원가입 시 예외가 발생한다")
        void signup_WithAdminRole_ThrowsException() {
            // given
            UserDto.SignupRequest request = createSignupRequest("admin@example.com", "관리자", "password123", UserRole.ROLE_ADMIN);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("관리자 계정은 직접 가입할 수 없습니다");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
        void signup_WithDuplicateEmail_ThrowsException() {
            // given
            UserDto.SignupRequest request = createSignupRequest("test@example.com", "중복유저", "password123", null);

            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 사용 중인 이메일입니다");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login() - 로그인")
    class LoginTest {

        @Test
        @DisplayName("정상적인 로그인이 성공한다")
        void login_WithValidCredentials_Success() {
            // given
            UserDto.LoginRequest request = createLoginRequest("test@example.com", "password123");

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtProvider.createAccessToken(eq(1L), eq("test@example.com"), eq(UserRole.ROLE_USER)))
                    .willReturn("accessToken");
            given(jwtProvider.createRefreshToken(1L)).willReturn("refreshToken");
            given(jwtProvider.getRefreshTokenExpiration()).willReturn(604800000L);
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

            // when
            UserDto.LoginResponse response = userService.login(request);

            // then
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
        void login_WithNonExistentEmail_ThrowsException() {
            // given
            UserDto.LoginRequest request = createLoginRequest("nonexistent@example.com", "password123");

            given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생한다")
        void login_WithWrongPassword_ThrowsException() {
            // given
            UserDto.LoginRequest request = createLoginRequest("test@example.com", "wrongPassword");

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("비활성화된 계정으로 로그인 시 예외가 발생한다")
        void login_WithDisabledAccount_ThrowsException() {
            // given
            User disabledUser = User.create(2L, "disabled@example.com", "비활성", "encodedPassword", UserRole.ROLE_USER);
            disabledUser.changeStatus(UserStatus.BLOCKED);

            UserDto.LoginRequest request = createLoginRequest("disabled@example.com", "password123");

            given(userRepository.findByEmail("disabled@example.com")).willReturn(Optional.of(disabledUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("비활성화된 계정입니다");
        }
    }

    @Nested
    @DisplayName("refresh() - 토큰 갱신")
    class RefreshTest {

        @Test
        @DisplayName("정상적인 토큰 갱신이 성공한다")
        void refresh_WithValidToken_Success() {
            // given
            String oldRefreshToken = "validRefreshToken";
            UserDto.RefreshRequest request = new UserDto.RefreshRequest(oldRefreshToken);

            RefreshToken storedToken = RefreshToken.create(oldRefreshToken, 1L, 604800000L);

            given(jwtProvider.validateToken(oldRefreshToken)).willReturn(true);
            given(refreshTokenRepository.findByToken(oldRefreshToken)).willReturn(Optional.of(storedToken));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(jwtProvider.createAccessToken(eq(1L), eq("test@example.com"), eq(UserRole.ROLE_USER)))
                    .willReturn("newAccessToken");
            given(jwtProvider.createRefreshToken(1L)).willReturn("newRefreshToken");
            given(jwtProvider.getRefreshTokenExpiration()).willReturn(604800000L);

            // when
            UserDto.TokenResponse response = userService.refresh(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refresh_WithInvalidToken_ThrowsException() {
            // given
            UserDto.RefreshRequest request = new UserDto.RefreshRequest("invalidToken");

            given(jwtProvider.validateToken("invalidToken")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.refresh(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 리프레시 토큰입니다");
        }
    }

    @Nested
    @DisplayName("getMe() / getUser() - 사용자 조회")
    class GetUserTest {

        @Test
        @DisplayName("내 정보를 조회할 수 있다")
        void getMe_WithExistingUser_ReturnsUserResponse() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserDto.UserResponse response = userService.getMe(1L);

            // then
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getNickname()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
        void getMe_WithNonExistingUser_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMe(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUser() - 사용자 정보 수정")
    class UpdateUserTest {

        @Test
        @DisplayName("닉네임을 수정할 수 있다")
        void updateUser_WithNewNickname_UpdatesSuccessfully() {
            // given
            UserDto.UpdateRequest request = createUpdateRequest("새닉네임", null);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserDto.UserResponse response = userService.updateUser(1L, request);

            // then
            assertThat(response.getNickname()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("비밀번호를 수정할 수 있다")
        void updateUser_WithNewPassword_UpdatesSuccessfully() {
            // given
            UserDto.UpdateRequest request = createUpdateRequest(null, "newPassword123");

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(passwordEncoder.encode("newPassword123")).willReturn("newEncodedPassword");

            // when
            userService.updateUser(1L, request);

            // then
            assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }
    }

    @Nested
    @DisplayName("deleteUser() - 회원 탈퇴")
    class DeleteUserTest {

        @Test
        @DisplayName("회원 탈퇴(Soft Delete)가 성공한다")
        void deleteUser_WithExistingUser_SoftDeletes() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            userService.deleteUser(1L);

            // then
            assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(testUser.isEnabled()).isFalse();
            assertThat(testUser.getDeletedAt()).isNotNull();

            verify(refreshTokenRepository).deleteByUserId(1L);
        }
    }

    @Nested
    @DisplayName("Admin API 테스트")
    class AdminApiTest {

        @Test
        @DisplayName("전체 사용자 목록을 페이징하여 조회할 수 있다")
        void getAllUsers_ReturnsPagedResults() {
            // given
            List<User> users = List.of(testUser);
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);

            given(userRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class))).willReturn(userPage);

            // when
            UserDto.UserPageResponse response = userService.getAllUsers(0, 10);

            // then
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("사용자 Role을 변경할 수 있다")
        void updateRole_ChangesUserRole() {
            // given
            UserDto.RoleUpdateRequest request = createRoleUpdateRequest(UserRole.ROLE_COMPANY);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserDto.AdminUserResponse response = userService.updateRole(1L, request);

            // then
            assertThat(response.getRole()).isEqualTo(UserRole.ROLE_COMPANY);
        }

        @Test
        @DisplayName("사용자를 차단하면 Refresh Token이 삭제된다")
        void updateStatus_ToBlocked_DeletesRefreshToken() {
            // given
            UserDto.StatusUpdateRequest request = createStatusUpdateRequest(UserStatus.BLOCKED);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            userService.updateStatus(1L, request);

            // then
            assertThat(testUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
            verify(refreshTokenRepository).deleteByUserId(1L);
        }

        @Test
        @DisplayName("사용자 통계를 조회할 수 있다")
        void getUserStats_ReturnsStatistics() {
            // given
            given(userRepository.count()).willReturn(100L);
            given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(80L);
            given(userRepository.countByStatus(UserStatus.BLOCKED)).willReturn(5L);
            given(userRepository.countByRole(UserRole.ROLE_ADMIN)).willReturn(3L);
            given(userRepository.countByRole(UserRole.ROLE_COMPANY)).willReturn(20L);
            given(userRepository.countByRole(UserRole.ROLE_USER)).willReturn(77L);

            // when
            UserDto.UserStatsResponse response = userService.getUserStats();

            // then
            assertThat(response.getTotalUsers()).isEqualTo(100L);
            assertThat(response.getActiveUsers()).isEqualTo(80L);
            assertThat(response.getBlockedUsers()).isEqualTo(5L);
            assertThat(response.getAdminCount()).isEqualTo(3L);
            assertThat(response.getCompanyCount()).isEqualTo(20L);
            assertThat(response.getUserCount()).isEqualTo(77L);
        }
    }

    // === Helper Methods ===

    private UserDto.SignupRequest createSignupRequest(String email, String nickname, String password, UserRole role) {
        UserDto.SignupRequest request = new UserDto.SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }

    private UserDto.LoginRequest createLoginRequest(String email, String password) {
        UserDto.LoginRequest request = new UserDto.LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private UserDto.UpdateRequest createUpdateRequest(String nickname, String password) {
        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private UserDto.RoleUpdateRequest createRoleUpdateRequest(UserRole role) {
        UserDto.RoleUpdateRequest request = new UserDto.RoleUpdateRequest();
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }

    private UserDto.StatusUpdateRequest createStatusUpdateRequest(UserStatus status) {
        UserDto.StatusUpdateRequest request = new UserDto.StatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
