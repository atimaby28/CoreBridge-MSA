package halo.corebridge.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GatewayAuthenticationFilter 테스트")
class GatewayAuthenticationFilterTest {

    private GatewayAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthenticationFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("성공: Gateway 헤더가 있으면 SecurityContext에 인증 정보가 설정된다")
    void withHeaders_setsAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("X-User-Id", "100");
        request.addHeader("X-User-Email", "user@test.com");
        request.addHeader("X-User-Role", "ROLE_USER");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(100L);
        assertThat(auth.getCredentials()).isEqualTo("user@test.com");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("성공: Gateway 헤더가 없으면 SecurityContext가 비어있다")
    void withoutHeaders_noAuthentication() throws ServletException, IOException {
        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("성공: ADMIN 역할도 올바르게 파싱된다")
    void adminRole_parsedCorrectly() throws ServletException, IOException {
        // given
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Email", "admin@test.com");
        request.addHeader("X-User-Role", "ROLE_ADMIN");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("실패: X-User-Role 없이 X-User-Id만 있으면 예외 발생")
    void onlyUserId_withoutRole_throws() {
        // given
        request.addHeader("X-User-Id", "100");

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> filter.doFilterInternal(request, response, filterChain)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("성공: 필터 후 항상 다음 필터가 호출된다")
    void filterChain_alwaysCalled() throws ServletException, IOException {
        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
