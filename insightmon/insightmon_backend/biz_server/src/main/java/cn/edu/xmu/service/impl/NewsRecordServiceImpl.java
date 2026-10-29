package cn.edu.xmu.service.impl;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.mapper.NewsRecordMapper;
import cn.edu.xmu.service.NewsRecordService;
import cn.edu.xmu.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.xmu.result.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsRecordServiceImpl extends ServiceImpl<NewsRecordMapper, NewsRecord> implements NewsRecordService {

    private final NewsRecordMapper newsRecordMapper;
    private final RedisUtil redisUtil;

    @Override
    public PageResult getNewsRecordsByIds(int page, int pageSize, List<Long> ids, String searchTerm, LocalDateTime startDatetime, LocalDateTime endDatetime) {
        String cacheKey = "newsRecords:" + ids.toString() + ":" + page + "_" + pageSize + "_" + (searchTerm!= null? searchTerm : "") + "_" + (startDatetime!= null? startDatetime.toString() : "nullStartDatetime") + "_" + (endDatetime!= null? endDatetime.toString() : "nullEndDatetime");
        // 先从 Redis 中尝试获取数据
        PageResult cacheResult = (PageResult) redisUtil.get(cacheKey);
        if (cacheResult!= null) {
            return cacheResult;
        }

        LambdaQueryWrapper<NewsRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(NewsRecord::getId, ids);  // 设置查询条件：id在给定的列表中

        // 如果 searchItem 不为空，添加模糊查询条件
        if (searchTerm != null && !searchTerm.isEmpty()) {
            queryWrapper.like(NewsRecord::getTitle, searchTerm);
        }

        // 如果 startDatetime 和 endDatetime 不为空，添加日期范围查询条件
        if (startDatetime != null) {
            queryWrapper.ge(NewsRecord::getPublishDatetime, startDatetime);
        }
        if (endDatetime != null) {
            queryWrapper.le(NewsRecord::getPublishDatetime, endDatetime);
        }


        // 使用 MyBatis-Plus 分页查询
        Page<NewsRecord> pageRequest = new Page<>(page, pageSize);
        Page<NewsRecord> resultPage = newsRecordMapper.selectPage(pageRequest, queryWrapper);

        // 封装为自定义 PageResult
        PageResult pageResult = new PageResult();
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setRecords(resultPage.getRecords());
        redisUtil.set(cacheKey, pageResult);
        return pageResult;
    }

}
