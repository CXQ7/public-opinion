package cn.edu.xmu.signalx.report.infrastructure.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.domain.model.ReportStatus;
import cn.edu.xmu.signalx.report.domain.repository.ReportRepository;
import cn.edu.xmu.signalx.report.infrastructure.converter.ReportConverter;
import cn.edu.xmu.signalx.report.infrastructure.mapper.ReportMapper;
import cn.edu.xmu.signalx.report.infrastructure.persistence.ReportPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {

    private final ReportMapper reportMapper;

    @Override
    public Long save(Report report) {
        ReportPO po = ReportConverter.toPO(report);
        if (report.getId() == null) {
            reportMapper.insert(po);
            // Reflect generated ID back to domain object so subsequent saves become updates.
            report.setId(po.getId());
        } else {
            reportMapper.updateById(po);
        }
        return po.getId();
    }

    @Override
    public Report findById(Long id) {
        ReportPO po = reportMapper.selectById(id);
        return ReportConverter.toDomain(po);
    }

    @Override
    public PageResult<Report> findByTaskId(Long taskId, int page, int size) {
        return findByTaskIdAndType(taskId, 0, page, size);
    }

    @Override
    public PageResult<Report> findByTaskIdAndType(Long taskId, Integer taskType, int page, int size) {
        Page<ReportPO> p = new Page<>(page, size);
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPO::getTaskId, taskId);
        if (taskType != null) {
            wrapper.eq(ReportPO::getTaskType, taskType);
        }
        wrapper.orderByDesc(ReportPO::getCreateTime);

        reportMapper.selectPage(p, wrapper);

        return new PageResult<>(
            p.getRecords().stream().map(ReportConverter::toDomain).collect(Collectors.toList()),
            p.getTotal()
        );
    }

    @Override
    public Report findLatestByTaskId(Long taskId) {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPO::getTaskId, taskId);
        wrapper.orderByDesc(ReportPO::getCreateTime);
        wrapper.last("LIMIT 1");
        ReportPO po = reportMapper.selectOne(wrapper);
        return ReportConverter.toDomain(po);
    }

    @Override
    public List<Report> findByStatus(ReportStatus status) {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPO::getStatus, status.getCode());
        return reportMapper.selectList(wrapper).stream()
                .map(ReportConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Report> findGeneratingWithCozeId() {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPO::getStatus, ReportStatus.GENERATING.getCode())
               .isNotNull(ReportPO::getCozeRunId);
        return reportMapper.selectList(wrapper).stream()
                .map(ReportConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Report findByTaskIdAndName(Long taskId, String name) {
        return findByTaskIdAndTypeAndName(taskId, 0, name);
    }

    @Override
    public Report findByTaskIdAndTypeAndName(Long taskId, Integer taskType, String name) {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPO::getTaskId, taskId)
               .eq(ReportPO::getTaskType, taskType)
               .eq(ReportPO::getName, name)
               .last("LIMIT 1");
        ReportPO po = reportMapper.selectOne(wrapper);
        return ReportConverter.toDomain(po);
    }

    @Override
    public void delete(Long id) {
        reportMapper.deleteById(id);
    }
}
