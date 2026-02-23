package halo.corebridge.user.exception;

import halo.corebridge.common.exception.BaseException;
import halo.corebridge.common.response.BaseResponseStatus;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long userId) {
        super(BaseResponseStatus.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId);
    }

}
