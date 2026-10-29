package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.vo.KeywordVO;
import cn.edu.xmu.mapper.KeywordMapper;
import cn.edu.xmu.result.PageResult;
import cn.edu.xmu.service.impl.KeywordServiceImpl;
import cn.edu.xmu.utils.ESUtil;
import cn.edu.xmu.utils.RedisUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

// 使用 MockitoExtension 自动初始化 Mockito
@ExtendWith(MockitoExtension.class)
// notification: 用例由AI生成, 未经人工校验
public class TestGetKeywordsList {
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private ESUtil esUtil;
    @Mock
    private KeywordMapper keywordMapper;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    private static final List<KeywordVO> MOCK_VO_LIST = Arrays.asList(
            createKeywordVO(1L, "测试关键词1", 100),
            createKeywordVO(2L, "测试关键词2", 200)
    );

    private static KeywordVO createKeywordVO(Long id, String keyword, int heat) {
        KeywordVO vo = new KeywordVO();
        vo.setId(id);
        vo.setKeyword(keyword);
        vo.setHeat(heat);
        return vo;
    }

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
            this.searchTerm = null;
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

    // JUnit 5 参数化测试的数据提供方法
    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(new TestCase("空搜索词").expectResult(2L, MOCK_VO_LIST)),
                Arguments.of(new TestCase("带搜索词").withSearchTerm("测试").expectResult(2L, MOCK_VO_LIST)),
                Arguments.of(new TestCase("缓存命中").expectResult(2L, MOCK_VO_LIST)),
                Arguments.of(new TestCase("ES连接超时").expectResult(2L, MOCK_VO_LIST).expectException("ES连接超时"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testGetKeywordsList(TestCase testCase) {
        if ("空搜索词".equals(testCase.desc) || "带搜索词".equals(testCase.desc)) {
            when(esUtil.parseResponse(any(), any())).thenReturn(new PageResult(2L, MOCK_VO_LIST));
        }

        // 配置缓存
        if ("缓存命中".equals(testCase.desc)) {
            when(redisUtil.get(anyString())).thenReturn(testCase.expected);
        }

        // 配置ES异常 (注意：原代码中使用的是"ES异常降级"，但数据中是"ES连接超时")
        if ("ES连接超时".equals(testCase.desc)) {
            when(esUtil.search(any(), any())).thenThrow(new RuntimeException("ES连接超时"));
        }

        if (testCase.expectedExceptionMsg != null) {
            // 测试异常情况
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keywordService.getKeywordsList(
                        testCase.page,
                        testCase.pageSize,
                        testCase.searchTerm
                );
            });
            assertTrue(exception.getMessage().contains(testCase.expectedExceptionMsg),
                    "Exception message should contain: " + testCase.expectedExceptionMsg +
                            ", but was: " + exception.getMessage());
        } else {
            // 测试正常情况
            PageResult result = keywordService.getKeywordsList(
                    testCase.page,
                    testCase.pageSize,
                    testCase.searchTerm
            );

            assertEquals(testCase.expected.getTotal(), result.getTotal());
            assertEquals(testCase.expected.getRecords().size(), result.getRecords().size());
        }
    }
}
