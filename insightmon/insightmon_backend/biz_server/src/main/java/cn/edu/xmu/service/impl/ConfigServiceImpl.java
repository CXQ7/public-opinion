package cn.edu.xmu.service.impl;

import cn.edu.xmu.mapper.ConfigMapper;
import cn.edu.xmu.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import cn.edu.xmu.domain.po.Config;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

}
