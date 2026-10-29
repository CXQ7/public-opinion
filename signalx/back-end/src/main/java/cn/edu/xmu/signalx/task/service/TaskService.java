package cn.edu.xmu.signalx.task.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.task.domain.dto.TaskDTO;
import cn.edu.xmu.signalx.task.domain.model.Slice;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import cn.edu.xmu.signalx.task.domain.vo.SliceVO;
import cn.edu.xmu.signalx.task.domain.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public Long createTask(Long userId, TaskDTO.Create cmd) {
        Task task = Task.create(userId, cmd.getKeywords(), cmd.getStartTime(), cmd.getEndTime(), cmd.getInterval());
        return taskRepository.save(task);
    }

    @Transactional
    public void updateTask(Long userId, Long taskId, TaskDTO.Update cmd) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Task not found");
        }
        task.validateOwnership(userId);
        task.updateConfig(cmd.getKeywords(), cmd.getStartTime(), cmd.getEndTime(), cmd.getInterval());
        taskRepository.save(task);
    }

    @Transactional
    public TaskStatus switchTask(Long userId, Long taskId, boolean enable) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Task not found");
        }
        task.validateOwnership(userId);
        task.switchStatus(enable);
        taskRepository.save(task);
        return task.getStatus();
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Task not found");
        }
        task.validateOwnership(userId);
        // Logical delete implementation depends on Repository.
        // Here we call delete, assuming repository handles it (e.g. Mybatis Plus TableLogic).
        taskRepository.delete(taskId);
    }

    public PageResult<TaskVO> getTasks(Long userId, int page, int size) {
        PageResult<Task> result = taskRepository.findByUserId(userId, page, size);
        return new PageResult<>(
            result.getList().stream().map(this::toVO).collect(Collectors.toList()),
            result.getTotal()
        );
    }

    public PageResult<SliceVO> getSlices(Long userId, TaskDTO.SliceQuery query) {
        // Optional: Check if user owns the task
        if (query.getTaskId() != null && userId != null) {
             Task task = taskRepository.findById(query.getTaskId());
             if (task != null) {
                 // Info expert: Task should check ownership, but task might be null if we just allow search
                 // Here we enforce ownership check if userId is provided
                 task.validateOwnership(userId);
             }
        }

        PageResult<Slice> result = taskRepository.findSlices(query.getTaskId(), query.getPage(), query.getSize());
        return new PageResult<>(
            result.getList().stream().map(this::toSliceVO).collect(Collectors.toList()),
            result.getTotal()
        );
    }

    private TaskVO toVO(Task task) {
        return TaskVO.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .keywords(task.getKeywords())
                .startTime(task.getConfig().getStartTime())
                .endTime(task.getConfig().getEndTime())
                .interval(task.getConfig().getInterval())
                .status(task.getStatus().getCode())
                .createTime(task.getCreateTime())
                .build();
    }

    private SliceVO toSliceVO(Slice slice) {
        return SliceVO.builder()
                .id(slice.getId())
                .taskId(slice.getTaskId())
                .title(slice.getTitle())
                .heat(slice.getHeat())
                .sentiment(slice.getSentiment().name())
                .summary(slice.getSummary())
                .eventTime(slice.getEventTime())
                .build();
    }
}
