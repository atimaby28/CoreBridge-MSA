package halo.corebridge.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found. userId=" + userId);
    }

    public UserNotFoundException(String email) {
        super("User not found. email=" + email);
    }
}
