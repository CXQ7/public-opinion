package cn.edu.xmu.service.impl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class ScheduledTasks {

    // 定时调用爬虫脚本的任务
//    @Scheduled(cron = "0 20 0 * * *")  // 每天 00:20:00 执行一次
//    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void runPythonScript() {
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder("python", "app.py");
//            Process process = processBuilder.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            int exitCode = process.waitFor();
//            System.out.println("Python script exited with code: " + exitCode);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
