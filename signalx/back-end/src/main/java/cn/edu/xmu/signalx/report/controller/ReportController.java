package cn.edu.xmu.signalx.report.controller;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.core.common.UserContext;
import cn.edu.xmu.signalx.report.controller.dto.ReportDTO;
import cn.edu.xmu.signalx.report.controller.dto.ReportDetailDTO;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ========== 新增：手动触发报表生成的测试接口（增加 taskType 支持） ==========
    @GetMapping("/test/trigger")
    public Result<?> testTriggerReport(@RequestParam Long taskId,
                                       @RequestParam(defaultValue = "0") Integer taskType) {
        try {
            reportService.triggerReportForTask(taskId, taskType);
            // 成功返回：使用Result.success(String msg)
            return Result.success("报表生成已触发，可查询report表查看结果");
        } catch (Exception e) {
            // 失败返回：匹配Result.fail的参数格式（code + msg）
            return Result.fail(500, "触发失败：" + e.getMessage());
        }
    }

    // ========== 原有接口（增加 taskType 以区分多态 ID） ==========
    @GetMapping("/list")
    public Result<PageResult<ReportDTO>> listReports(@RequestParam Long taskId,
                                                     @RequestParam(defaultValue = "0") Integer taskType,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        PageResult<Report> result = reportService.listReports(userId, taskId, taskType, page, size);

        List<ReportDTO> dtos = result.getList().stream().map(r -> {
            ReportDTO dto = new ReportDTO();
            dto.setId(r.getId());
            dto.setTaskId(r.getTaskId());
            dto.setName(r.getName());
            dto.setStatus(r.getStatus().getCode());
            dto.setCreateTime(r.getCreateTime());
            return dto;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(dtos, result.getTotal()));
    }

    @GetMapping("/{id}")
    public Result<ReportDetailDTO> getReport(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        Report report = reportService.getReportDetail(userId, id);

        ReportDetailDTO dto = new ReportDetailDTO();
        dto.setId(report.getId());
        dto.setTaskId(report.getTaskId());
        dto.setName(report.getName());
        dto.setSummary(report.getSummary());
        dto.setStatus(report.getStatus().getCode());
        dto.setCreateTime(report.getCreateTime());

        return Result.success(dto);
    }

    @PostMapping("/{id}/delete")
    public Result<Boolean> deleteReportPost(@PathVariable Long id) {
         return deleteReport(id);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteReport(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        reportService.deleteReport(userId, id);
        return Result.success(true);
    }

    @PostMapping("/{id}/pushEmail")
    public Result<Boolean> pushEmail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(reportService.pushReportEmail(userId, id));
    }
}
