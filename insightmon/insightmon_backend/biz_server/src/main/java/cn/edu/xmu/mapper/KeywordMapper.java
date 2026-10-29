package cn.edu.xmu.mapper;

import cn.edu.xmu.domain.po.Keyword;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KeywordMapper extends BaseMapper<Keyword> {
    // 通过 XML 定义映射，无需添加注解
    List<Keyword> getKeywordsList(@Param("searchTerm") String searchTerm);

    @Select("SELECT * FROM keyword ORDER BY heat DESC LIMIT 10")
    List<Keyword> selectTop10KeywordsByHeat();
}
