package cn.edu.xmu.controller;

import cn.edu.xmu.result.Result;
import cn.edu.xmu.service.impl.ScheduledTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/tasks")
public class ScheduledTaskController {

    private final ScheduledTasks scheduledTasks;

    @Autowired
    public ScheduledTaskController(ScheduledTasks scheduledTasks) {
        this.scheduledTasks = scheduledTasks;
    }

    @GetMapping("/run-script")
    public Result<String> runScriptManually() {
        scheduledTasks.runPythonScript();
        return Result.success("Python script executed successfully");
    }
}
