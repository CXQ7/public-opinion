package cn.edu.xmu;

import cn.edu.xmu.utils.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootTest
@Slf4j
class PublicEyeApplicationTests {

//    @Autowired
//    private PasswordEncoder passwordEncoder;

    @Test
    // 定时调用爬虫脚本的任务
    //@Scheduled(cron = "*/1 * * * * ?")
    public void runPythonScript() {
        try {
            // 调用 Python 脚本
            ProcessBuilder processBuilder = new ProcessBuilder("python", "weibo.py");
            Process process = processBuilder.start();

            // 输出爬虫脚本的结果（可选）
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Python script exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void encodePassword() {
        String rawPassword = "__CONFIGURE_LOCALLY__";
        String encodedPassword = HashUtil.hashPassword(rawPassword);
        log.info("原始密码: {}, 加密后密码: {}", rawPassword, encodedPassword);
    }

}
