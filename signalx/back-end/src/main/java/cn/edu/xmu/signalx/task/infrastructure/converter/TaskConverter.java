package cn.edu.xmu.signalx.task.infrastructure.converter;

import cn.edu.xmu.signalx.task.domain.model.*;
import cn.edu.xmu.signalx.task.infrastructure.persistence.SlicePO;
import cn.edu.xmu.signalx.task.infrastructure.persistence.TaskPO;

import java.util.ArrayList;

public class TaskConverter {

    public static Task toDomain(TaskPO po) {
        if (po == null) {
            return null;
        }
        TaskConfig config = new TaskConfig(po.getStartTime(), po.getEndTime(), po.getInterval());
        return Task.reconstruct(
                po.getId(),
                po.getUserId(),
                po.getKeywords() != null ? po.getKeywords() : new ArrayList<>(),
                config,
                TaskStatus.fromCode(po.getStatus()),
                po.getCreateTime(),
                po.getNextRunTime()
        );
    }

    public static TaskPO toPO(Task task) {
        if (task == null) {
            return null;
        }
        TaskPO po = new TaskPO();
        po.setId(task.getId());
        po.setUserId(task.getUserId());
        po.setKeywords(task.getKeywords());
        if (task.getConfig() != null) {
            po.setStartTime(task.getConfig().getStartTime());
            po.setEndTime(task.getConfig().getEndTime());
            po.setInterval(task.getConfig().getInterval());
        }
        if (task.getStatus() != null) {
            po.setStatus(task.getStatus().getCode());
        }
        po.setNextRunTime(task.getNextRunTime());
        return po;
    }

    public static Slice toSliceDomain(SlicePO po) {
        if (po == null) return null;
        return Slice.reconstruct(
                po.getId(),
                po.getTaskId(),
                po.getTitle(),
                po.getHeat(),
                po.getSentiment() != null ? Sentiment.valueOf(po.getSentiment()) : Sentiment.NEUTRAL,
                po.getSummary(),
                po.getEventTime()
        );
    }

    public static SlicePO toSlicePO(Slice slice) {
        if (slice == null) return null;
        SlicePO po = new SlicePO();
        po.setId(slice.getId());
        po.setTaskId(slice.getTaskId());
        po.setTitle(slice.getTitle());
        po.setHeat(slice.getHeat());
        po.setSentiment(slice.getSentiment() != null ? slice.getSentiment().name() : Sentiment.NEUTRAL.name());
        po.setSummary(slice.getSummary());
        po.setEventTime(slice.getEventTime());
        return po;
    }
}
