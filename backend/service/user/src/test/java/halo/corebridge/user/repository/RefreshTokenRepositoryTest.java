package halo.corebridge.user.repository;

import halo.corebridge.user.model.entity.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken savedToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        savedToken = refreshTokenRepository.save(
                RefreshToken.create("test-refresh-token", 1L, 604800000L)
        );
    }

    @Nested
    @DisplayName("findByToken()")
    class FindByTokenTest {

        @Test
        @DisplayName("존재하는 토큰으로 RefreshToken을 찾을 수 있다")
        void findByToken_WithExistingToken_ReturnsRefreshToken() {
            // when
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("test-refresh-token");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("test-refresh-token");
            assertThat(result.get().getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 조회하면 빈 Optional을 반환한다")
        void findByToken_WithNonExistingToken_ReturnsEmpty() {
            // when
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("non-existing-token");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserIdTest {

        @Test
        @DisplayName("존재하는 userId로 RefreshToken을 찾을 수 있다")
        void findByUserId_WithExistingUserId_ReturnsRefreshToken() {
            // when
            Optional<RefreshToken> result = refreshTokenRepository.findByUserId(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 userId로 조회하면 빈 Optional을 반환한다")
        void findByUserId_WithNonExistingUserId_ReturnsEmpty() {
            // when
            Optional<RefreshToken> result = refreshTokenRepository.findByUserId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByUserId()")
    class DeleteByUserIdTest {

        @Test
        @DisplayName("userId로 RefreshToken을 삭제할 수 있다")
        void deleteByUserId_WithExistingUserId_DeletesToken() {
            // given
            assertThat(refreshTokenRepository.findByUserId(1L)).isPresent();

            // when
            refreshTokenRepository.deleteByUserId(1L);

            // then
            assertThat(refreshTokenRepository.findByUserId(1L)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 userId로 삭제해도 예외가 발생하지 않는다")
        void deleteByUserId_WithNonExistingUserId_DoesNotThrow() {
            // when & then - 예외가 발생하지 않아야 함
            refreshTokenRepository.deleteByUserId(999L);

            // 기존 데이터는 그대로 유지됨
            assertThat(refreshTokenRepository.findByUserId(1L)).isPresent();
        }
    }

    @Nested
    @DisplayName("토큰 업데이트 테스트")
    class TokenUpdateTest {

        @Test
        @DisplayName("토큰을 업데이트할 수 있다")
        void updateToken_ChangesTokenValue() {
            // given
            RefreshToken token = refreshTokenRepository.findByUserId(1L).orElseThrow();
            String newTokenValue = "new-refresh-token";

            // when
            token.updateToken(newTokenValue, 604800000L);
            refreshTokenRepository.save(token);

            // then
            RefreshToken updatedToken = refreshTokenRepository.findByUserId(1L).orElseThrow();
            assertThat(updatedToken.getToken()).isEqualTo(newTokenValue);
        }

        @Test
        @DisplayName("새 토큰으로 기존 토큰을 조회할 수 없다")
        void updateToken_OldTokenNotFound() {
            // given
            RefreshToken token = refreshTokenRepository.findByUserId(1L).orElseThrow();
            String oldTokenValue = token.getToken();

            // when
            token.updateToken("updated-token", 604800000L);
            refreshTokenRepository.save(token);

            // then
            assertThat(refreshTokenRepository.findByToken(oldTokenValue)).isEmpty();
            assertThat(refreshTokenRepository.findByToken("updated-token")).isPresent();
        }
    }

    @Nested
    @DisplayName("여러 사용자 토큰 관리")
    class MultipleUsersTest {

        @BeforeEach
        void setUpMultipleUsers() {
            refreshTokenRepository.save(RefreshToken.create("user2-token", 2L, 604800000L));
            refreshTokenRepository.save(RefreshToken.create("user3-token", 3L, 604800000L));
        }

        @Test
        @DisplayName("여러 사용자의 토큰을 독립적으로 관리할 수 있다")
        void multipleUsers_IndependentTokenManagement() {
            // when
            refreshTokenRepository.deleteByUserId(2L);

            // then
            assertThat(refreshTokenRepository.findByUserId(1L)).isPresent();
            assertThat(refreshTokenRepository.findByUserId(2L)).isEmpty();
            assertThat(refreshTokenRepository.findByUserId(3L)).isPresent();
        }

        @Test
        @DisplayName("전체 토큰 수를 확인할 수 있다")
        void count_ReturnsCorrectNumber() {
            // when
            long count = refreshTokenRepository.count();

            // then
            assertThat(count).isEqualTo(3);
        }
    }
}
