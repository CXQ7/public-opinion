package cn.edu.xmu.signalx.user.infrastructure.mapper;

import cn.edu.xmu.signalx.user.infrastructure.po.UserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
