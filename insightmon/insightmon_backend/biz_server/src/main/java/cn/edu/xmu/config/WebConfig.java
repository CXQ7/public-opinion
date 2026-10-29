package cn.edu.xmu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 允许所有路径的请求
//                .allowedOrigins("http://localhost:7000") // 允许来自这个域的请求
//                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的HTTP方法
//                .allowedHeaders("*") // 允许所有头
//                .allowCredentials(true); // 允许携带认证信息（如cookies）
//    }

}
