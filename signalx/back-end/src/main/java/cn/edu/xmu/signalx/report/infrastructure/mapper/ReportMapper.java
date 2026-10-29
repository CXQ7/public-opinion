package cn.edu.xmu.signalx.report.infrastructure.mapper;

import cn.edu.xmu.signalx.report.infrastructure.persistence.ReportPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper extends BaseMapper<ReportPO> {
}
