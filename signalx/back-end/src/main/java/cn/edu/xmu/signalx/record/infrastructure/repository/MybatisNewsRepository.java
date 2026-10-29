package cn.edu.xmu.signalx.record.infrastructure.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.domain.repository.NewsRepository;
import cn.edu.xmu.signalx.record.infrastructure.converter.NewsConverter;
import cn.edu.xmu.signalx.record.infrastructure.mapper.NewsMapper;
import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor // Autowire Mapper
public class MybatisNewsRepository implements NewsRepository {

    private final NewsMapper newsMapper;

    @Override
    public void save(News news) {
        NewsPO po = NewsConverter.toPO(news);
        if (po.getId() == null) {
            newsMapper.insert(po);
            news.setId(po.getId());
        } else {
            newsMapper.updateById(po);
        }
    }

    @Override
    public News findById(Long id) {
        NewsPO po = newsMapper.selectById(id);
        return NewsConverter.toDomain(po);
    }

    @Override
    public PageResult<News> findByCondition(Long taskId, Integer taskType, Long sliceId, String keyword, int page, int size) {
        Page<NewsPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<NewsPO> wrapper = new LambdaQueryWrapper<>();

        if (taskId != null) {
            wrapper.eq(NewsPO::getTaskId, taskId);
        }
        if (taskType != null) {
            wrapper.eq(NewsPO::getTaskType, taskType);
        }
        if (sliceId != null) {
            wrapper.eq(NewsPO::getSliceId, sliceId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(NewsPO::getTitle, keyword)
                   .or()
                   .like(NewsPO::getContent, keyword);
        }
        wrapper.orderByDesc(NewsPO::getPublishTime);

        // Execute query
        Page<NewsPO> poPage = newsMapper.selectPage(pageParam, wrapper);

        // Convert page result
        return new PageResult<>(
                poPage.getRecords().stream().map(NewsConverter::toDomain).collect(Collectors.toList()),
                poPage.getTotal()
        );
    }
}
