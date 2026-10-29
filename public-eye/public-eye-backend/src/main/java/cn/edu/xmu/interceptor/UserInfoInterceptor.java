package cn.edu.xmu.interceptor;

import cn.edu.xmu.utils.JwtTool;
import cn.edu.xmu.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


@Slf4j
@RequiredArgsConstructor
@Component
public class UserInfoInterceptor implements HandlerInterceptor {
    private final JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 放行 /users/login 接口
        String requestURI = request.getRequestURI();
        if ("/users/login".equals(requestURI) || "/keyword/list".equals(requestURI)) {
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader("Authorization");
        //2、校验令牌
        Long userId = jwtTool.parseToken(token);
        UserContext.setUser(userId);
        //3、通过，放行
        return true;

    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        // 移除用户
        UserContext.removeUser();
    }
}
