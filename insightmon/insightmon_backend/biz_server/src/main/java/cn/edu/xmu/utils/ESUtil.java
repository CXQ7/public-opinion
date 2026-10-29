package cn.edu.xmu.utils;

import cn.edu.xmu.result.PageResult;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ESUtil {
    private final RestHighLevelClient client;

    public void addDocument(String indexName, Object object) throws IOException {
        IndexRequest request = new IndexRequest(indexName);
        String jsonString = JSON.toJSONString(object);
        request.source(jsonString, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    public void addDocumentBulk(String indexName, List<Object> objects) throws IOException {
        BulkRequest request = new BulkRequest(indexName);
        for (Object o : objects) {
            request.add(new IndexRequest()
                    .source(JSONUtil.toJsonStr(o), XContentType.JSON));
        }
        // 3.发送请求
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            System.out.println("Bulk insert had failures: " + response.buildFailureMessage());
        }
    }

    public PageResult queryDocument(String indexName, Class<?> clazz) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        request.source()
                .query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return parseResponse(response, clazz);
    }

    public PageResult parseResponse(SearchResponse response, Class<?> clazz){
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        List records = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            records.add(JSONUtil.toBean(hit.getSourceAsString(), clazz));
        }
        PageResult pageResult = new PageResult();
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        return pageResult;
    }


    public SearchResponse search(SearchRequest request, RequestOptions options) {
        try {
            return client.search(request, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
