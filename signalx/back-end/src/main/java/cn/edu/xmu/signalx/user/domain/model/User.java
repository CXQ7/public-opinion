package cn.edu.xmu.signalx.user.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
public class User {
    @Setter
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private UserRole role;
    private UserStatus status;

    // Factory method for creating a new user
    public static User createNormal(String username, String password, String email) {
        if (StrUtil.isBlank(username)) {
            throw new DomainException("Username cannot be empty");
        }
        if (StrUtil.isBlank(password)) {
            throw new DomainException("Password cannot be empty");
        }
        User params = new User();
        params.username = username;
        params.passwordHash = BCrypt.hashpw(password);
        params.email = email;
        params.role = UserRole.USER; // Enforce USER role
        params.status = UserStatus.NORMAL;
        return params;
    }

    // Reconstruct from persistence (Package-private or public, used by converter)
    public static User reconstruct(Long id, String username, String passwordHash, String email, UserRole role, UserStatus status) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.passwordHash = passwordHash;
        user.email = email;
        user.role = role;
        user.status = status;
        return user;
    }

    // Behavior: Update basic info
    public void updateBasicInfo(String newUsername, String newEmail) {
        if (this.status == UserStatus.DISABLED) {
            throw new DomainException("User is disabled");
        }
        if (StrUtil.isNotBlank(newUsername)) {
            this.username = newUsername;
        }
        if (StrUtil.isNotBlank(newEmail)) {
            this.email = newEmail;
        }
        // Role update is intentionally excluded here
    }

    // Behavior: Verify password
    public boolean verifyPassword(String rawPassword) {
        return BCrypt.checkpw(rawPassword, this.passwordHash);
    }
}
