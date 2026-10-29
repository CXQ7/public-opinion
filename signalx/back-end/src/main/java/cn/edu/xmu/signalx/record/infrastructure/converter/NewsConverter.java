package cn.edu.xmu.signalx.record.infrastructure.converter;

import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;

public class NewsConverter {

    public static News toDomain(NewsPO po) {
        if (po == null) {
            return null;
        }
        return News.reconstruct(
                po.getId(),
                po.getTaskId(),
                po.getTaskType(),
                po.getSliceId(),
                po.getTitle(),
                po.getContent(),
                po.getUrl(),
                po.getSource(),
                po.getPublishTime(),
                po.getSentiment()
        );
    }

    public static NewsPO toPO(News news) {
        if (news == null) {
            return null;
        }
        NewsPO po = new NewsPO();
        po.setId(news.getId());
        po.setTaskId(news.getTaskId());
        po.setTaskType(news.getTaskType());
        po.setSliceId(news.getSliceId());
        po.setTitle(news.getTitle());
        po.setContent(news.getContent());
        po.setUrl(news.getUrl());
        po.setSource(news.getSource());
        po.setSentiment(news.getSentiment());
        po.setPublishTime(news.getPublishTime());
        // createTime is usually handled by DB default or MyBatis hook, leaving null here is fine if DB has default
        return po;
    }
}
