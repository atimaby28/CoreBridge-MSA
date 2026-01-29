package halo.corebridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.user.model.dto.UserDto;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import halo.corebridge.user.security.JwtProperties;
import halo.corebridge.user.security.JwtProvider;
import halo.corebridge.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Nested
    @DisplayName("POST /api/v1/users/signup - 회원가입")
    class SignupTest {

        @Test
        @DisplayName("정상적인 회원가입 요청이 성공한다")
        void signup_WithValidRequest_ReturnsCreated() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "newuser@example.com",
                    "nickname": "신규유저",
                    "password": "password123"
                }
                """;

            UserDto.UserResponse response = UserDto.UserResponse.builder()
                    .userId(1L)
                    .email("newuser@example.com")
                    .nickname("신규유저")
                    .role(UserRole.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.signup(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.result.nickname").value("신규유저"))
                    .andExpect(jsonPath("$.result.role").value("ROLE_USER"));
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 400 에러를 반환한다")
        void signup_WithInvalidEmail_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "invalid-email",
                    "nickname": "테스터",
                    "password": "password123"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400 에러를 반환한다")
        void signup_WithShortPassword_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "test@example.com",
                    "nickname": "테스터",
                    "password": "short"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("닉네임이 2자 미만이면 400 에러를 반환한다")
        void signup_WithShortNickname_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "test@example.com",
                    "nickname": "A",
                    "password": "password123"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 필드가 없으면 400 에러를 반환한다")
        void signup_WithMissingFields_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/login - 로그인")
    class LoginTest {

        @Test
        @DisplayName("정상적인 로그인 요청이 성공한다")
        void login_WithValidCredentials_ReturnsSuccess() throws Exception {
            // given
            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

            UserDto.LoginResponse response = UserDto.LoginResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .role(UserRole.ROLE_USER)
                    .accessToken("accessToken")
                    .refreshToken("refreshToken")
                    .build();

            given(userService.login(any())).willReturn(response);
            given(jwtProperties.getAccessTokenExpiration()).willReturn(1800000L);
            given(jwtProperties.getRefreshTokenExpiration()).willReturn(604800000L);

            // when & then
            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.email").value("test@example.com"))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("이메일이 없으면 400 에러를 반환한다")
        void login_WithMissingEmail_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "password": "password123"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me - 내 정보 조회")
    class GetMeTest {

        @Test
        @DisplayName("내 정보를 조회할 수 있다")
        void getMe_ReturnsUserInfo() throws Exception {
            // given
            UserDto.UserResponse response = UserDto.UserResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .role(UserRole.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.getMe(any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.email").value("test@example.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId} - 특정 회원 조회")
    class GetUserTest {

        @Test
        @DisplayName("특정 회원 정보를 조회할 수 있다")
        void getUser_WithValidUserId_ReturnsUserInfo() throws Exception {
            // given
            UserDto.UserResponse response = UserDto.UserResponse.builder()
                    .userId(123L)
                    .email("user123@example.com")
                    .nickname("사용자123")
                    .role(UserRole.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.getUser(123L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/123"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.userId").value(123))
                    .andExpect(jsonPath("$.result.email").value("user123@example.com"));
        }
    }

    @Nested
    @DisplayName("Admin API 테스트")
    class AdminApiTest {

        @Test
        @DisplayName("전체 사용자 목록을 조회할 수 있다")
        void getAllUsers_ReturnsUserList() throws Exception {
            // given
            UserDto.AdminUserResponse adminUser = UserDto.AdminUserResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserDto.UserPageResponse response = UserDto.UserPageResponse.builder()
                    .users(List.of(adminUser))
                    .totalCount(1L)
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(userService.getAllUsers(0, 20)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/admin/list")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.users").isArray())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
        }

        @Test
        @DisplayName("사용자 통계를 조회할 수 있다")
        void getUserStats_ReturnsStats() throws Exception {
            // given
            UserDto.UserStatsResponse response = UserDto.UserStatsResponse.builder()
                    .totalUsers(100L)
                    .activeUsers(80L)
                    .blockedUsers(5L)
                    .adminCount(3L)
                    .companyCount(20L)
                    .userCount(77L)
                    .build();

            given(userService.getUserStats()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/admin/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalUsers").value(100))
                    .andExpect(jsonPath("$.result.activeUsers").value(80))
                    .andExpect(jsonPath("$.result.blockedUsers").value(5));
        }

        @Test
        @DisplayName("사용자를 검색할 수 있다")
        void searchUsers_WithKeyword_ReturnsMatchingUsers() throws Exception {
            // given
            UserDto.AdminUserResponse user = UserDto.AdminUserResponse.builder()
                    .userId(1L)
                    .email("john@example.com")
                    .nickname("John Doe")
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserDto.UserPageResponse response = UserDto.UserPageResponse.builder()
                    .users(List.of(user))
                    .totalCount(1L)
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(userService.searchUsers(eq("john"), eq(0), eq(20))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/admin/search")
                            .param("keyword", "john")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.users[0].email").value("john@example.com"));
        }

        @Test
        @DisplayName("특정 Role의 사용자 목록을 조회할 수 있다")
        void getUsersByRole_ReturnsFilteredList() throws Exception {
            // given
            UserDto.AdminUserResponse companyUser = UserDto.AdminUserResponse.builder()
                    .userId(1L)
                    .email("company@example.com")
                    .nickname("기업회원")
                    .role(UserRole.ROLE_COMPANY)
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserDto.UserPageResponse response = UserDto.UserPageResponse.builder()
                    .users(List.of(companyUser))
                    .totalCount(1L)
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(userService.getUsersByRole(eq(UserRole.ROLE_COMPANY), eq(0), eq(20))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/admin/list/role/ROLE_COMPANY")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.users[0].role").value("ROLE_COMPANY"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me - 내 정보 수정")
    class UpdateMeTest {

        @Test
        @DisplayName("닉네임을 수정할 수 있다")
        void updateMe_WithValidRequest_ReturnsUpdatedInfo() throws Exception {
            // given
            String requestBody = """
                {
                    "nickname": "새닉네임"
                }
                """;

            UserDto.UserResponse response = UserDto.UserResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("새닉네임")
                    .role(UserRole.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.updateUser(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.nickname").value("새닉네임"));
        }

        @Test
        @DisplayName("닉네임이 너무 길면 400 에러를 반환한다")
        void updateMe_WithTooLongNickname_ReturnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "nickname": "이닉네임은스무자를초과하는아주아주긴닉네임입니다"
                }
                """;

            // when & then
            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/me - 회원 탈퇴")
    class DeleteMeTest {

        @Test
        @DisplayName("회원 탈퇴가 성공한다")
        void deleteMe_ReturnsSuccess() throws Exception {
            // given
            doNothing().when(userService).deleteUser(any());

            // when & then
            mockMvc.perform(delete("/api/v1/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/admin/{userId}/role - 사용자 Role 변경")
    class UpdateRoleTest {

        @Test
        @DisplayName("사용자 Role을 변경할 수 있다")
        void updateRole_ReturnsUpdatedUser() throws Exception {
            // given
            String requestBody = """
                {
                    "role": "ROLE_COMPANY"
                }
                """;

            UserDto.AdminUserResponse response = UserDto.AdminUserResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .role(UserRole.ROLE_COMPANY)
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.updateRole(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/admin/1/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.role").value("ROLE_COMPANY"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/admin/{userId}/status - 사용자 Status 변경")
    class UpdateStatusTest {

        @Test
        @DisplayName("사용자를 차단할 수 있다")
        void updateStatus_ToBlocked_ReturnsUpdatedUser() throws Exception {
            // given
            String requestBody = """
                {
                    "status": "BLOCKED"
                }
                """;

            UserDto.AdminUserResponse response = UserDto.AdminUserResponse.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.BLOCKED)
                    .enabled(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(userService.updateStatus(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/admin/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("BLOCKED"))
                    .andExpect(jsonPath("$.result.enabled").value(false));
        }
    }
}
