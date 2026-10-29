package cn.edu.xmu.signalx.task.domain.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.task.domain.model.Slice;
import cn.edu.xmu.signalx.task.domain.model.Task;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository {
    // Task
    Long save(Task task);
    Task findById(Long id);
    PageResult<Task> findByUserId(Long userId, int page, int size);
    /**
     * Find all ACTIVE tasks.
     */
    List<Task> findActiveTasks();
    List<Task> findDueTasks(LocalDateTime dueTime);
    void delete(Long id);

    // Slice
    PageResult<Slice> findSlices(Long taskId, int page, int size);
    // 简单起见，这里不需要单独的SliceRepository
}
