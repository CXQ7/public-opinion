package cn.edu.xmu.signalx.task.infrastructure.converter;

import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.model.TaskConfig;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.infrastructure.persistence.OpenclawTaskPO;

public class OpenclawTaskConverter {

    public static OpenclawTask toDomain(OpenclawTaskPO po) {
        if (po == null) {
            return null;
        }
        TaskConfig config = new TaskConfig(po.getStartTime(), po.getEndTime(), po.getInterval());
        return OpenclawTask.reconstruct(
                po.getId(),
                po.getUserId(),
                po.getName(),
                po.getUrl(),
                po.getCrawlLogic(),
                config,
                TaskStatus.fromCode(po.getStatus()),
                po.getCreateTime(),
                po.getNextRunTime()
        );
    }

    public static OpenclawTaskPO toPO(OpenclawTask task) {
        if (task == null) {
            return null;
        }
        OpenclawTaskPO po = new OpenclawTaskPO();
        po.setId(task.getId());
        po.setUserId(task.getUserId());
        po.setName(task.getName());
        po.setUrl(task.getUrl());
        po.setCrawlLogic(task.getCrawlLogic());
        if (task.getConfig() != null) {
            po.setStartTime(task.getConfig().getStartTime());
            po.setEndTime(task.getConfig().getEndTime());
            po.setInterval(task.getConfig().getInterval());
        }
        if (task.getStatus() != null) {
            po.setStatus(task.getStatus().getCode());
        }
        po.setNextRunTime(task.getNextRunTime());
        po.setCreateTime(task.getCreateTime());
        return po;
    }
}
