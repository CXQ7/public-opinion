package cn.edu.xmu.signalx.task.infrastructure.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository;
import cn.edu.xmu.signalx.task.infrastructure.converter.OpenclawTaskConverter;
import cn.edu.xmu.signalx.task.infrastructure.mapper.OpenclawTaskMapper;
import cn.edu.xmu.signalx.task.infrastructure.persistence.OpenclawTaskPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OpenclawTaskRepositoryImpl implements OpenclawTaskRepository {

    private final OpenclawTaskMapper openclawTaskMapper;

    @Override
    public Long save(OpenclawTask task) {
        OpenclawTaskPO po = OpenclawTaskConverter.toPO(task);
        if (po.getId() == null) {
            openclawTaskMapper.insert(po);
        } else {
            openclawTaskMapper.updateById(po);
        }
        return po.getId();
    }

    @Override
    public OpenclawTask findById(Long id) {
        OpenclawTaskPO po = openclawTaskMapper.selectById(id);
        return OpenclawTaskConverter.toDomain(po);
    }

    @Override
    public PageResult<OpenclawTask> findByUserId(Long userId, int page, int size) {
        Page<OpenclawTaskPO> result = openclawTaskMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<OpenclawTaskPO>()
                        .eq(OpenclawTaskPO::getUserId, userId)
                        .orderByDesc(OpenclawTaskPO::getCreateTime)
        );

        List<OpenclawTask> data = result.getRecords().stream()
                .map(OpenclawTaskConverter::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(data, result.getTotal());
    }

    @Override
    public List<OpenclawTask> findDueTasks(LocalDateTime dueTime) {
        List<OpenclawTaskPO> pos = openclawTaskMapper.selectList(
                new LambdaQueryWrapper<OpenclawTaskPO>()
                        .eq(OpenclawTaskPO::getStatus, 1) // ACTIVE
                        .le(OpenclawTaskPO::getNextRunTime, dueTime)
        );
        return pos.stream().map(OpenclawTaskConverter::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        openclawTaskMapper.deleteById(id);
    }
}
