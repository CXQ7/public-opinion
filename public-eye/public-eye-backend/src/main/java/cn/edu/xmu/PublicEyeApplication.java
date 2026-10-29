package cn.edu.xmu;

import cn.edu.xmu.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
@MapperScan("cn.edu.xmu.mapper")
public class PublicEyeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicEyeApplication.class, args);
    }

}
