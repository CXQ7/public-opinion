package cn.edu.xmu.mapper;

import cn.edu.xmu.domain.po.Event;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventMapper extends BaseMapper<Event> {

    // 查询某个关键词下的所有事件，按事件日期升序排列
    @Select("SELECT * FROM event WHERE keyword_id = #{keywordId} ORDER BY event_datetime ASC")
    List<Event> getEventsByKeywordId(Long keywordId);

    // 查询某个关键词下最早的事件
    @Select("SELECT * FROM event WHERE keyword_id = #{keywordId} ORDER BY event_datetime ASC LIMIT 1")
    Event getEarliestEventByKeywordId(Long keywordId);
}
