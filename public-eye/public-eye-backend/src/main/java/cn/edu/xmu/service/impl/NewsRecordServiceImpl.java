package cn.edu.xmu.service.impl;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.mapper.NewsRecordMapper;
import cn.edu.xmu.service.NewsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsRecordServiceImpl extends ServiceImpl<NewsRecordMapper, NewsRecord> implements NewsRecordService {

    private final NewsRecordMapper NewsRecordMapper;
}
