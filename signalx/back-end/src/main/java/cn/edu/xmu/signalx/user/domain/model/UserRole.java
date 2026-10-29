package cn.edu.xmu.signalx.user.domain.model;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole parse(String role) {
        if (role == null) return USER;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
