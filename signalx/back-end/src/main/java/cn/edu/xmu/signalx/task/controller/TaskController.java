package cn.edu.xmu.signalx.task.controller;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.core.common.UserContext;
import cn.edu.xmu.signalx.task.domain.dto.TaskDTO;
import cn.edu.xmu.signalx.task.domain.vo.SliceVO;
import cn.edu.xmu.signalx.task.domain.vo.TaskVO;
import cn.edu.xmu.signalx.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // --- Task Endpoints ---

    @PostMapping("/task/create")
    public Result<Map<String, String>> create(@RequestBody TaskDTO.Create request) {
        Long userId = UserContext.getUserId();
        Long taskId = taskService.createTask(userId, request);
        return Result.success(Map.of("taskId", taskId.toString()));
    }

    @GetMapping("/task/list")
    public Result<PageResult<TaskVO>> list(@RequestParam(required = false) Long userId,
                                           @RequestParam int page,
                                           @RequestParam int size) {
        Long currentUserId = UserContext.getUserId();
        // 如果是普通用户，强制只能查自己
        // 这里简单处理：如果没有传 userId，或者传了但不是自己（且不是管理员，这里暂不判断管理员），则查自己
        // 出于安全考虑，通常只允许查自己的。
        // 如果需要管理员查看所有，需配合 Role 判断。
        // 根据 API 设计，list 接口 query: userId, page, size.
        // 假设这里目前只服务于当前登录用户。

        // 覆盖查询条件中的 userId 为当前登录用户，确保数据隔离
        // 如果业务允许管理员查看他人，需要判断 UserContext.getUserRole()

        return Result.success(taskService.getTasks(currentUserId, page, size));
    }

    @PostMapping("/task/{taskId}/update")
    public Result<Boolean> update(@PathVariable Long taskId, @RequestBody TaskDTO.Update request) {
        Long userId = UserContext.getUserId();
        taskService.updateTask(userId, taskId, request);
        return Result.success(true);
    }

    @PostMapping("/task/{taskId}/switch")
    public Result<Map<String, String>> switchStatus(@PathVariable Long taskId, @RequestBody TaskDTO.Switch request) {
        Long userId = UserContext.getUserId();
        var status = taskService.switchTask(userId, taskId, request.getEnable());
        return Result.success(Map.of("status", status.name())); // ACTIVE / INACTIVE
    }

    @PostMapping("/task/{taskId}/delete")
    public Result<Boolean> delete(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        taskService.deleteTask(userId, taskId);
        return Result.success(true);
    }

    // --- Analysis / Slices Endpoints ---

    @GetMapping("/analysis/slices")
    public Result<PageResult<SliceVO>> listSlices(@RequestParam Long taskId,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam int page,
                                                  @RequestParam int size) {
        // API Document says query: taskId, userId, page, size
        // We prioritize enforcing security
        Long currentUserId = UserContext.getUserId();

        TaskDTO.SliceQuery query = new TaskDTO.SliceQuery();
        query.setPage(page);
        query.setSize(size);
        query.setTaskId(taskId);
        query.setUserId(currentUserId);

        return Result.success(taskService.getSlices(currentUserId, query));
    }
}
