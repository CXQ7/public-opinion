package cn.edu.xmu.signalx.task.infrastructure.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.task.domain.model.Slice;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import cn.edu.xmu.signalx.task.infrastructure.converter.TaskConverter;
import cn.edu.xmu.signalx.task.infrastructure.mapper.SliceMapper;
import cn.edu.xmu.signalx.task.infrastructure.mapper.TaskMapper;
import cn.edu.xmu.signalx.task.infrastructure.persistence.SlicePO;
import cn.edu.xmu.signalx.task.infrastructure.persistence.TaskPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;
    private final SliceMapper sliceMapper;

    @Override
    public Long save(Task task) {
        TaskPO po = TaskConverter.toPO(task);
        if (task.getId() == null) {
            taskMapper.insert(po);
            // Reflect ID back if needed, or just return it.
            // In DDD, we usually let database generate ID and we get it back.
            // But Task object is immutable structurally (except fields).
            // We can set ID via reflection or just rely on return value to update upstream.
            // Here we rely on PO id being populated by Mybatis.
        } else {
            taskMapper.updateById(po);
        }
        return po.getId();
    }

    @Override
    public Task findById(Long id) {
        TaskPO po = taskMapper.selectById(id);
        return TaskConverter.toDomain(po);
    }

    @Override
    public PageResult<Task> findByUserId(Long userId, int page, int size) {
        Page<TaskPO> p = new Page<>(page, size);
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(TaskPO::getUserId, userId);
        }
        wrapper.orderByDesc(TaskPO::getCreateTime);

        taskMapper.selectPage(p, wrapper);

        return new PageResult<>(
            p.getRecords().stream().map(TaskConverter::toDomain).collect(Collectors.toList()),
            p.getTotal()
        );
    }

    @Override
    public List<Task> findActiveTasks() {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getStatus, 1) // ACTIVE
               .orderByDesc(TaskPO::getCreateTime);
        return taskMapper.selectList(wrapper).stream()
                .map(TaskConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.List<Task> findDueTasks(java.time.LocalDateTime dueTime) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getStatus, 1) // ACTIVE
               .le(TaskPO::getNextRunTime, dueTime);
        return taskMapper.selectList(wrapper).stream()
                .map(TaskConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        taskMapper.deleteById(id);
    }

    @Override
    public PageResult<Slice> findSlices(Long taskId, int page, int size) {
        Page<SlicePO> p = new Page<>(page, size);
        LambdaQueryWrapper<SlicePO> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(SlicePO::getTaskId, taskId);
        }
        wrapper.orderByDesc(SlicePO::getEventTime);

        sliceMapper.selectPage(p, wrapper);

        return new PageResult<>(
            p.getRecords().stream().map(TaskConverter::toSliceDomain).collect(Collectors.toList()),
            p.getTotal()
        );
    }
}
