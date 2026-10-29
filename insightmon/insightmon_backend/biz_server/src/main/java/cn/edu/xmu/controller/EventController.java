package cn.edu.xmu.controller;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.mapper.EventMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "事件相关接口")
@RestController
@RequestMapping("/user/event")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class EventController {

    private final EventMapper eventMapper;


}
