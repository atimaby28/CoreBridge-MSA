package halo.corebridge.user.repository;

import halo.corebridge.user.model.entity.User;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser = userRepository.save(
                User.create(1L, "test@example.com", "테스터", "password123", UserRole.ROLE_USER)
        );
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTest {

        @Test
        @DisplayName("존재하는 이메일로 사용자를 찾을 수 있다")
        void findByEmail_WithExistingEmail_ReturnsUser() {
            // when
            Optional<User> result = userRepository.findByEmail("test@example.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
            assertThat(result.get().getNickname()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
        void findByEmail_WithNonExistingEmail_ReturnsEmpty() {
            // when
            Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmailTest {

        @Test
        @DisplayName("존재하는 이메일이면 true를 반환한다")
        void existsByEmail_WithExistingEmail_ReturnsTrue() {
            // when
            boolean exists = userRepository.existsByEmail("test@example.com");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 false를 반환한다")
        void existsByEmail_WithNonExistingEmail_ReturnsFalse() {
            // when
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreatedAtDesc()")
    class FindAllOrderByCreatedAtDescTest {

        @BeforeEach
        void setUpMultipleUsers() {
            userRepository.save(
                    User.create(2L, "user2@example.com", "사용자2", "password", UserRole.ROLE_USER)
            );
            userRepository.save(
                    User.create(3L, "user3@example.com", "사용자3", "password", UserRole.ROLE_COMPANY)
            );
        }

        @Test
        @DisplayName("생성일 기준 내림차순으로 페이징된 사용자 목록을 조회한다")
        void findAll_ReturnsPagedResults() {
            // when
            Page<User> result = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 2));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findByRoleOrderByCreatedAtDesc()")
    class FindByRoleTest {

        @BeforeEach
        void setUpUsersWithDifferentRoles() {
            userRepository.save(
                    User.create(2L, "company1@example.com", "기업1", "password", UserRole.ROLE_COMPANY)
            );
            userRepository.save(
                    User.create(3L, "company2@example.com", "기업2", "password", UserRole.ROLE_COMPANY)
            );
            userRepository.save(
                    User.create(4L, "admin@example.com", "관리자", "password", UserRole.ROLE_ADMIN)
            );
        }

        @Test
        @DisplayName("특정 역할의 사용자만 조회한다")
        void findByRole_ReturnsUsersWithSpecificRole() {
            // when
            Page<User> companies = userRepository.findByRoleOrderByCreatedAtDesc(
                    UserRole.ROLE_COMPANY, PageRequest.of(0, 10)
            );

            // then
            assertThat(companies.getContent()).hasSize(2);
            assertThat(companies.getContent())
                    .allMatch(user -> user.getRole() == UserRole.ROLE_COMPANY);
        }

        @Test
        @DisplayName("ROLE_USER 역할의 사용자만 조회한다")
        void findByRole_ReturnsOnlyRoleUsers() {
            // when
            Page<User> users = userRepository.findByRoleOrderByCreatedAtDesc(
                    UserRole.ROLE_USER, PageRequest.of(0, 10)
            );

            // then
            assertThat(users.getContent()).hasSize(1);
            assertThat(users.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("searchByKeyword()")
    class SearchByKeywordTest {

        @BeforeEach
        void setUpSearchData() {
            userRepository.save(
                    User.create(2L, "john@example.com", "John Doe", "password", UserRole.ROLE_USER)
            );
            userRepository.save(
                    User.create(3L, "jane@example.com", "Jane Smith", "password", UserRole.ROLE_USER)
            );
        }

        @Test
        @DisplayName("이메일로 사용자를 검색할 수 있다")
        void searchByKeyword_WithEmail_ReturnsMatchingUsers() {
            // when
            Page<User> result = userRepository.searchByKeyword("john", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("닉네임으로 사용자를 검색할 수 있다")
        void searchByKeyword_WithNickname_ReturnsMatchingUsers() {
            // when
            Page<User> result = userRepository.searchByKeyword("Smith", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNickname()).isEqualTo("Jane Smith");
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
        void searchByKeyword_WithNoMatch_ReturnsEmptyPage() {
            // when
            Page<User> result = userRepository.searchByKeyword("nonexistent", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByStatus() 및 countByRole()")
    class CountTest {

        @BeforeEach
        void setUpUsersForCount() {
            userRepository.save(
                    User.create(2L, "blocked@example.com", "차단된사용자", "password", UserRole.ROLE_USER)
            );
            User blockedUser = userRepository.findByEmail("blocked@example.com").orElseThrow();
            blockedUser.changeStatus(UserStatus.BLOCKED);
            userRepository.save(blockedUser);

            userRepository.save(
                    User.create(3L, "company@example.com", "기업회원", "password", UserRole.ROLE_COMPANY)
            );
        }

        @Test
        @DisplayName("상태별 사용자 수를 조회할 수 있다")
        void countByStatus_ReturnsCorrectCount() {
            // when
            Long activeCount = userRepository.countByStatus(UserStatus.ACTIVE);
            Long blockedCount = userRepository.countByStatus(UserStatus.BLOCKED);

            // then
            assertThat(activeCount).isEqualTo(2); // test@example.com, company@example.com
            assertThat(blockedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("역할별 사용자 수를 조회할 수 있다")
        void countByRole_ReturnsCorrectCount() {
            // when
            Long userCount = userRepository.countByRole(UserRole.ROLE_USER);
            Long companyCount = userRepository.countByRole(UserRole.ROLE_COMPANY);
            Long adminCount = userRepository.countByRole(UserRole.ROLE_ADMIN);

            // then
            assertThat(userCount).isEqualTo(2); // test@example.com, blocked@example.com
            assertThat(companyCount).isEqualTo(1);
            assertThat(adminCount).isEqualTo(0);
        }
    }
}
