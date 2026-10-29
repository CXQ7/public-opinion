package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.domain.vo.KeywordVO;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.mapper.KeywordMapper;
import cn.edu.xmu.result.PageResult;
import cn.edu.xmu.service.impl.KeywordServiceImpl;
import cn.edu.xmu.utils.RedisUtil;
import cn.hutool.core.bean.BeanUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 使用MockitoExtension初始化Mockito
@ExtendWith(MockitoExtension.class)
// notification: 用例由AI生成, 未经人工校验
public class TestGetKeywordsListNoElastic {
    @Mock
    private KeywordMapper keywordMapper;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    private static Keyword keyword(Long id, String keyword) {
        Keyword k = new Keyword();
        k.setId(id);
        k.setKeyword(keyword);
        return k;
    }

    private static final List<Keyword> MOCK_KEYWORDS = Arrays.asList(
            keyword(1L, "测试关键词1"),
            keyword(2L, "测试关键词2")
    );

    static class TestCase {
        final String desc;
        final int page;
        final int pageSize;
        String searchTerm;
        final PageResult expected;
        String expectedExceptionMsg;

        TestCase(String desc) {
            this.desc = desc;
            this.page = 1;
            this.pageSize = 10;
            this.expected = new PageResult(0L, Collections.emptyList());
        }

        TestCase withSearchTerm(String term) {
            this.searchTerm = term;
            return this;
        }

        TestCase expectResult(long total, List<KeywordVO> data) {
            this.expected.setTotal(total);
            this.expected.setRecords(data);
            return this;
        }

        TestCase expectException(String msg) {
            this.expectedExceptionMsg = msg;
            return this;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    // 提供测试参数的方法
    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(new TestCase("正常分页查询").expectResult(2L, convertToVOList(MOCK_KEYWORDS))),
                Arguments.of(new TestCase("空结果查询").withSearchTerm("不存在").expectResult(0L, Collections.emptyList())),
                Arguments.of(new TestCase("带搜索词查询").withSearchTerm("测试").expectResult(2L, convertToVOList(MOCK_KEYWORDS))),
                Arguments.of(new TestCase("数据库异常").expectException("数据库连接失败"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testGetKeywordsListNoElastic(TestCase testCase) {
        // 配置正常数据库响应
        when(keywordMapper.getKeywordsList(any()))
                .thenReturn(testCase.desc.equals("正常分页查询") ? MOCK_KEYWORDS :
                        testCase.desc.equals("空结果查询") ? Collections.emptyList() : MOCK_KEYWORDS);

        // 配置数据库异常
        if (testCase.desc.equals("数据库异常")) {
            when(keywordMapper.getKeywordsList(any())).thenThrow(new RuntimeException("数据库连接失败"));
        }

        if (testCase.expectedExceptionMsg != null && !testCase.expectedExceptionMsg.isEmpty()) {
            // 测试异常情况
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keywordService.getKeywordsListNoElastic(
                        testCase.page,
                        testCase.pageSize,
                        testCase.searchTerm
                );
            });
            assertTrue(exception.getMessage().startsWith(testCase.expectedExceptionMsg), "异常消息为" + exception.getMessage() + "但应以指定内容开头: " + testCase.expectedExceptionMsg);
        } else {
            // 测试正常情况
            PageResult result = keywordService.getKeywordsListNoElastic(
                    testCase.page,
                    testCase.pageSize,
                    testCase.searchTerm
            );

            assertEquals(testCase.expected.getTotal(), result.getTotal());
            assertEquals(testCase.expected.getRecords(), result.getRecords());
        }
    }

    private static List<KeywordVO> convertToVOList(List<Keyword> keywords) {
        return keywords.stream().map(k -> {
            KeywordVO vo = new KeywordVO();
            BeanUtil.copyProperties(k, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
