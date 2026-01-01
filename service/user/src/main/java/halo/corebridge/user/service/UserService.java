package halo.corebridge.user.service;

import halo.corebridge.infra.id.snowflake.Snowflake;
import halo.corebridge.user.exception.UserNotFoundException;
import halo.corebridge.user.model.dto.UserDto;
import halo.corebridge.user.model.entity.User;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.repostitory.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final Snowflake snowflake = new Snowflake();
    private final UserRepository userRepository;

    /**
     * 회원 생성
     */
    @Transactional
    public UserDto.UserResponse create(UserDto.UserCreateRequest request) {
        User user = userRepository.save(
                User.create(
                        snowflake.nextId(),
                        request.getEmail(),
                        request.getNickname(),
                        request.getPassword(),
                        UserRole.ROLE_USER
                )
        );

        return UserDto.UserResponse.from(user);
    }

    /**
     * 회원 수정
     */
    @Transactional
    public UserDto.UserResponse update(Long userId, UserDto.UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getNickname() != null) {
            user.updateProfile(request.getNickname());
        }

        if (request.getPassword() != null) {
            // TODO: Security 적용 시 passwordEncoder.encode()
            user.updatePassword(request.getPassword());
        }

        return UserDto.UserResponse.from(user);
    }

    /**
     * 회원 조회
     */
    @Transactional
    public UserDto.UserResponse read(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserDto.UserResponse.from(user);
    }

    /**
     * 회원 삭제
     */
    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        userRepository.delete(user);
    }
}
