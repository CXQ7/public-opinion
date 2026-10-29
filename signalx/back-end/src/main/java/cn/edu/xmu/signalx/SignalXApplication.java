package cn.edu.xmu.signalx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("cn.edu.xmu.signalx.*.infrastructure.mapper")
public class SignalXApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalXApplication.class, args);
    }

}
