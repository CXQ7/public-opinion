package cn.edu.xmu.signalx.record.infrastructure.mapper;

import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsMapper extends BaseMapper<NewsPO> {
}
