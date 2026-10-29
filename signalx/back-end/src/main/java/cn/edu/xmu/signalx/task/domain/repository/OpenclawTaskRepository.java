package cn.edu.xmu.signalx.task.domain.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;

import java.time.LocalDateTime;
import java.util.List;

public interface OpenclawTaskRepository {
    Long save(OpenclawTask task);
    OpenclawTask findById(Long id);
    PageResult<OpenclawTask> findByUserId(Long userId, int page, int size);
    List<OpenclawTask> findDueTasks(LocalDateTime dueTime);
    void delete(Long id);
}
