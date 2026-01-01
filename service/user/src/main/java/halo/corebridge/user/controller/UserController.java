package halo.corebridge.user.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.common.web.ApiPaths;
import halo.corebridge.user.model.dto.UserDto;
import halo.corebridge.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.USERS)
public class UserController {

    private final UserService userService;

    /**
     * 회원 생성
     */
    @PostMapping
    public BaseResponse<UserDto.UserResponse> create(
            @RequestBody UserDto.UserCreateRequest request
    ) {
        return BaseResponse.success(userService.create(request));
    }


    /**
     * 회원 조회
     */
    @GetMapping("/{userId}")
    public UserDto.UserResponse read(@PathVariable Long userId) {
        return userService.read(userId);
    }

    /**
     * 회원 정보 수정
     * - nickname, password 중 전달된 값만 수정
     */
    @PutMapping("/{userId}")
    public UserDto.UserResponse update(
            @PathVariable Long userId,
            @RequestBody UserDto.UserUpdateRequest request
    ) {
        return userService.update(userId, request);
    }

    /**
     * 회원 삭제
     */
    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
        userService.delete(userId);
    }
}
