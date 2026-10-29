package cn.edu.xmu.signalx.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间为 60 秒
        factory.setConnectTimeout(60000);
        // 设置读取超时时间为 300 秒
        factory.setReadTimeout(300000);
        return new RestTemplate(factory);
    }
}
