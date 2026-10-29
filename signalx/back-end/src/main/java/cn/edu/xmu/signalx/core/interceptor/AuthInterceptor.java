package cn.edu.xmu.signalx.core.interceptor;

import cn.edu.xmu.signalx.core.common.UserContext;
import cn.edu.xmu.signalx.core.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Header 中的 Authorization
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            response.setStatus(401);
            response.getWriter().write("Unauthorized: Missing Token");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            // 2. 验证并解析 Token
            if (JwtUtils.isTokenExpired(token)) {
                response.setStatus(401);
                response.getWriter().write("Unauthorized: Token Expired");
                return false;
            }

            Claims claims = JwtUtils.parseToken(token);
            Long userId = claims.get("id", Long.class);
            String role = claims.get("role", String.class);

            // 3. 存入线程上下文
            UserContext.setUserId(userId);
            UserContext.setUserRole(role);

            return true;
        } catch (Exception e) {
            log.error("Token validation failed", e);
            response.setStatus(401);
            response.getWriter().write("Unauthorized: Invalid Token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理线程上下文，防止内存泄漏
        UserContext.clear();
    }
}
