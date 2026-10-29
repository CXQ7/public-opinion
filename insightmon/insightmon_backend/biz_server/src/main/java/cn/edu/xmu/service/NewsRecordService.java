package cn.edu.xmu.service;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsRecordService extends IService<NewsRecord> {

    /**
     * 根据ids分页查询新闻记录
     *
     * @param ids   需要查询的新闻记录id列表
     * @param page  分页参数
     * @return 分页后的新闻记录
     */
    public PageResult getNewsRecordsByIds(int page, int pageSize, List<Long> ids, String searchTerm, LocalDateTime startDatetime, LocalDateTime endDatetime);
}
