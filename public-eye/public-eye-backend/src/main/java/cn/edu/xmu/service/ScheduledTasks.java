package cn.edu.xmu.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class ScheduledTasks {

    // 定时调用爬虫脚本的任务
    @Scheduled(cron = "0 */1 * * * ?")  // 每隔一分钟执行一次
    public void runPythonScript() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "weibo.py");
            Process process = processBuilder.start();

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
}
