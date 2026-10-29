package cn.edu.xmu.signalx.report.infrastructure.converter;

import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.infrastructure.persistence.ReportPO;

public class ReportConverter {
    public static Report toDomain(ReportPO po) {
        if (po == null) return null;
        return Report.reconstruct(
            po.getId(),
            po.getTaskId(),
            po.getTaskType(),
            po.getName(),
            po.getSummary(),
            po.getCozeRunId(),
            po.getFileUrl(),
            po.getStatus(),
            po.getCreateTime()
        );
    }

    public static ReportPO toPO(Report report) {
        if (report == null) return null;
        ReportPO po = new ReportPO();
        po.setId(report.getId());
        po.setTaskId(report.getTaskId());
        po.setTaskType(report.getTaskType());
        po.setName(report.getName());
        po.setSummary(report.getSummary());
        po.setCozeRunId(report.getCozeRunId());
        po.setFileUrl(report.getFileUrl());
        po.setStatus(report.getStatus().getCode());
        // createTime handled by DB/Model logic or MP
        return po;
    }
}
