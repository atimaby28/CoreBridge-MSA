package halo.corebridge.user.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.user.model.dto.UserDto;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.security.JwtProperties;
import halo.corebridge.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // ============================================
    // 인증 API (Public)
    // ============================================

    /**
     * 회원가입
     * POST /api/v1/users/signup
     */
    @PostMapping("/signup")
    public BaseResponse<UserDto.UserResponse> signup(@Valid @RequestBody UserDto.SignupRequest request) {
        return BaseResponse.success(userService.signup(request));
    }

    /**
     * 로그인
     * POST /api/v1/users/login
     * - Access Token, Refresh Token을 HttpOnly Cookie로 설정
     */
    @PostMapping("/login")
    public BaseResponse<UserDto.LoginResponse> login(
            @Valid @RequestBody UserDto.LoginRequest request,
            HttpServletResponse response) {
        UserDto.LoginResponse loginResponse = userService.login(request);

        // HttpOnly Cookie 설정
        setTokenCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());

        // 응답에서 토큰 제거 (Cookie로만 전달)
        return BaseResponse.success(loginResponse.withoutTokens());
    }

    /**
     * 토큰 갱신
     * POST /api/v1/users/refresh
     * - Cookie에서 Refresh Token 추출
     */
    @PostMapping("/refresh")
    public BaseResponse<UserDto.TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        // Cookie에서 Refresh Token 추출
        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 없습니다.");
        }

        UserDto.RefreshRequest refreshRequest = new UserDto.RefreshRequest(refreshToken);
        UserDto.TokenResponse tokenResponse = userService.refresh(refreshRequest);

        // 새 토큰으로 Cookie 갱신
        setTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());

        return BaseResponse.success(tokenResponse.withoutTokens());
    }

    /**
     * 로그아웃
     * POST /api/v1/users/logout
     * - Cookie 삭제
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {
        userService.logout(userId);

        // Cookie 삭제
        clearTokenCookies(response);

        return BaseResponse.success();
    }

    // ============================================
    // Cookie 유틸리티
    // ============================================

    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token Cookie (30분)
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(false)  // 개발환경: false, 프로덕션: true (HTTPS)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();

        // Refresh Token Cookie (7일)
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)  // 즉시 만료
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // ============================================
    // 사용자 API (Authenticated)
    // ============================================

    /**
     * 내 정보 조회
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public BaseResponse<UserDto.UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success(userService.getMe(userId));
    }

    /**
     * 내 정보 수정
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public BaseResponse<UserDto.UserResponse> updateMe(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return BaseResponse.success(userService.updateUser(userId, request));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/v1/users/me
     */
    @DeleteMapping("/me")
    public BaseResponse<Void> deleteMe(@AuthenticationPrincipal Long userId) {
        userService.deleteUser(userId);
        return BaseResponse.success();
    }

    /**
     * 특정 회원 조회
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public BaseResponse<UserDto.UserResponse> getUser(@PathVariable Long userId) {
        return BaseResponse.success(userService.getUser(userId));
    }

    // ============================================
    // Admin API (ROLE_ADMIN only)
    // ============================================

    /**
     * 전체 사용자 목록 조회
     * GET /api/v1/users/admin/list
     */
    @GetMapping("/admin/list")
    public BaseResponse<UserDto.UserPageResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(userService.getAllUsers(page, size));
    }

    /**
     * Role별 사용자 목록 조회
     * GET /api/v1/users/admin/list/role/{role}
     */
    @GetMapping("/admin/list/role/{role}")
    public BaseResponse<UserDto.UserPageResponse> getUsersByRole(
            @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(userService.getUsersByRole(role, page, size));
    }

    /**
     * 사용자 검색
     * GET /api/v1/users/admin/search
     */
    @GetMapping("/admin/search")
    public BaseResponse<UserDto.UserPageResponse> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(userService.searchUsers(keyword, page, size));
    }

    /**
     * 사용자 상세 조회 (Admin)
     * GET /api/v1/users/admin/{userId}
     */
    @GetMapping("/admin/{userId}")
    public BaseResponse<UserDto.AdminUserResponse> getAdminUserDetail(@PathVariable Long userId) {
        return BaseResponse.success(userService.getAdminUserDetail(userId));
    }

    /**
     * 사용자 Role 변경
     * PATCH /api/v1/users/admin/{userId}/role
     */
    @PatchMapping("/admin/{userId}/role")
    public BaseResponse<UserDto.AdminUserResponse> updateRole(
            @PathVariable Long userId,
            @RequestBody UserDto.RoleUpdateRequest request) {
        return BaseResponse.success(userService.updateRole(userId, request));
    }

    /**
     * 사용자 Status 변경
     * PATCH /api/v1/users/admin/{userId}/status
     */
    @PatchMapping("/admin/{userId}/status")
    public BaseResponse<UserDto.AdminUserResponse> updateStatus(
            @PathVariable Long userId,
            @RequestBody UserDto.StatusUpdateRequest request) {
        return BaseResponse.success(userService.updateStatus(userId, request));
    }

    /**
     * 사용자 통계
     * GET /api/v1/users/admin/stats
     */
    @GetMapping("/admin/stats")
    public BaseResponse<UserDto.UserStatsResponse> getUserStats() {
        return BaseResponse.success(userService.getUserStats());
    }
}
