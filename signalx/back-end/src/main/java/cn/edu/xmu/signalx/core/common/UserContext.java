package cn.edu.xmu.signalx.core.common;

/**
 * 线程上下文，用于存储当前登录用户的信息
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setUserRole(String role) {
        USER_ROLE_HOLDER.set(role);
    }

    public static String getUserRole() {
        return USER_ROLE_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
    }
}
