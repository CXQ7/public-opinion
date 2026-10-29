package cn.edu.xmu.signalx.report.domain.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.report.domain.model.Report;
import java.util.List;

public interface ReportRepository {
    Long save(Report report);
    Report findById(Long id);
    PageResult<Report> findByTaskId(Long taskId, int page, int size);
    PageResult<Report> findByTaskIdAndType(Long taskId, Integer taskType, int page, int size);
    Report findLatestByTaskId(Long taskId);
    List<Report> findByStatus(cn.edu.xmu.signalx.report.domain.model.ReportStatus status);
    List<Report> findGeneratingWithCozeId();
    /** Find a report by task and name for deduplication */
    Report findByTaskIdAndName(Long taskId, String name);
    Report findByTaskIdAndTypeAndName(Long taskId, Integer taskType, String name);
    void delete(Long id);
}
