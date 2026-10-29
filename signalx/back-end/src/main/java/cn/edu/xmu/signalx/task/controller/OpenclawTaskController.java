package cn.edu.xmu.signalx.task.controller;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.core.common.UserContext;
import cn.edu.xmu.signalx.task.domain.dto.OpenclawTaskDTO;
import cn.edu.xmu.signalx.task.domain.vo.OpenclawTaskVO;
import cn.edu.xmu.signalx.task.service.OpenclawTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/openclaw-task")
@RequiredArgsConstructor
public class OpenclawTaskController {

    private final OpenclawTaskService openclawTaskService;

    @PostMapping("/create")
    public Result<Long> create(@RequestBody OpenclawTaskDTO.Create request) {
        Long userId = UserContext.getUserId();
        Long id = openclawTaskService.createOpenclawTask(userId, request);
        return Result.success(id);
    }

    @PostMapping("/{taskId}/update")
    public Result<Boolean> update(@PathVariable Long taskId, @RequestBody OpenclawTaskDTO.Update request) {
        Long userId = UserContext.getUserId();
        openclawTaskService.updateOpenclawTask(userId, taskId, request);
        return Result.success(true);
    }

    @PostMapping("/{taskId}/switch")
    public Result<Map<String, String>> switchStatus(@PathVariable Long taskId, @RequestBody OpenclawTaskDTO.Switch request) {
        Long userId = UserContext.getUserId();
        var status = openclawTaskService.switchOpenclawTask(userId, taskId, request.getEnable());
        return Result.success(Map.of("status", status.name()));
    }

    @PostMapping("/{taskId}/delete")
    public Result<Boolean> delete(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        openclawTaskService.deleteOpenclawTask(userId, taskId);
        return Result.success(true);
    }

    @GetMapping("/list")
    public Result<PageResult<OpenclawTaskVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(openclawTaskService.getOpenclawTasks(userId, page, size));
    }
}
