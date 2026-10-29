package cn.edu.xmu.service.impl;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.service.EventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {

}
