package cn.edu.xmu.signalx.task.infrastructure.mapper;

import cn.edu.xmu.signalx.task.infrastructure.persistence.TaskPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskPO> {
}
