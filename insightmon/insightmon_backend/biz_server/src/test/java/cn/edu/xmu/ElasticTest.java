package cn.edu.xmu;

import cn.edu.xmu.domain.doc.EventDoc;
import cn.edu.xmu.domain.doc.KeywordVODoc;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.vo.KeywordVO;
import cn.edu.xmu.result.PageResult;
import cn.edu.xmu.service.KeywordService;
import cn.edu.xmu.utils.ESUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import com.alibaba.fastjson.JSON;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
@Slf4j
public class ElasticTest {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private ESUtil esUtil;

    @Test
    void testInsertDoc() throws IOException {
        PageResult keywordsList = keywordService.getKeywordsList(1, 10, null);
//        log.info("list: {}", keywordsList);
        for (Object record : keywordsList.getRecords()) {
            String jsonString = JSON.toJSONString(record);
            log.info("{}", jsonString);
            esUtil.addDocument("keyword_vo", record);
        }
    }

    @Test
    void testInsertBulk() throws IOException {
        int page = 1, pageSize = 1000;
        while(true){
            System.out.println("page: " + page);
            List<Object> records = keywordService.getKeywordsListNoElastic(page, pageSize, null).getRecords();
            if(records.isEmpty())
                return;
            List<Object> docs = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Object record : records) {
                KeywordVO vo = (KeywordVO) record;
                Event event = vo.getRepresentativeEvent();
                EventDoc eventDoc = BeanUtil.copyProperties(event, EventDoc.class);
                KeywordVODoc doc = BeanUtil.copyProperties(vo, KeywordVODoc.class);

                doc.setCreateTime(vo.getCreateTime().format(formatter));
                doc.setUpdateTime(vo.getUpdateTime().format(formatter));

                eventDoc.setCreateTime(event.getCreateTime().format(formatter));
                eventDoc.setUpdateTime(event.getUpdateTime().format(formatter));
                eventDoc.setEventDatetime(event.getEventDatetime().format(formatter));

                doc.setRepresentativeEvent(eventDoc);
                docs.add(doc);
            }
            esUtil.addDocumentBulk("keyword_vo", docs);
            page++;
        }
    }

    @Test
    void testQuery() throws IOException {
        esUtil.queryDocument("keyword_vo", KeywordVO.class);
    }

    @Test
    void testMatch() throws IOException {
        // 1.创建Request
        SearchRequest request = new SearchRequest("keyword_vo");
        // 2.组织请求参数
        request.source().query(QueryBuilders.multiMatchQuery("特朗普", "keyword", "representativeEvent.description"));
        // 2.2.高亮条件
        request.source().highlighter(
                SearchSourceBuilder.highlight()
                        .field("keyword")
                        .field("representativeEvent.description")
                        .preTags("<em>")
                        .postTags("</em>")
        );
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        esUtil.parseResponse(response, KeywordVO.class);
    }

    @Test
    void testConnection(){
        // 获取所有连接的主机信息
        // 获取 RestClient 实例
        RestClient restClient = client.getLowLevelClient();
        // 获取所有连接的主机信息
        List<Node> nodes = restClient.getNodes();
        for (Node node : nodes) {
            System.out.println("Connected to Elasticsearch host: " + node.getHost());
        }
        System.out.println("client = " + client);
    }

    static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    void testCreateIndex() throws IOException {
        // 1.创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("items");
        // 2.准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteIndex() throws IOException {
        // 创建 DeleteIndexRequest
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("items");
        client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
    }

//    @Test
//    void testIndexDoc() throws IOException{
//        Item item = itemService.getById(100000011127L);
//        //转为文档数据
//        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
//        //准备Request
//        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
//        //准备请求参数
//        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
//        //发送请求
//        client.index(request, RequestOptions.DEFAULT);
//    }
//
//    @Test
//    void testGetDocumentById() throws IOException {
//        //创建request对象
//        GetRequest request = new GetRequest("items", "100000011127");
//        //发送请求，得到结果
//        GetResponse response = client.get(request, RequestOptions.DEFAULT);
//        //解析
//        String json = response.getSourceAsString();
//        ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
//        System.out.println("doc = " + doc);
//    }
//
//    @Test
//    void testDeleteDocumentById() throws IOException {
//        //创建request对象
//        DeleteRequest request = new DeleteRequest("items", "100000011127");
//        //发送请求，得到结果
//        client.delete(request, RequestOptions.DEFAULT);
//    }
//
//    @Test
//    void testLoadItemDocs() throws IOException {
//        // 分页查询商品数据
//        int pageNo = 1;
//        int size = 1000;
//        while (true) {
//            Page<Item> page = itemService.lambdaQuery().eq(Item::getStatus, 1).page(new Page<Item>(pageNo, size));
//            // 非空校验
//            List<Item> items = page.getRecords();
//            if (CollUtils.isEmpty(items)) {
//                return;
//            }
//            log.info("加载第{}页数据，共{}条", pageNo, items.size());
//            // 1.创建Request
//            BulkRequest request = new BulkRequest("items");
//            // 2.准备参数，添加多个新增的Request
//            for (Item item : items) {
//                // 2.1.转换为文档类型ItemDTO
//                ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
//                // 2.2.创建新增文档的Request对象
//                request.add(new IndexRequest()
//                        .id(itemDoc.getId())
//                        .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
//            }
//            // 3.发送请求
//            client.bulk(request, RequestOptions.DEFAULT);
//
//            // 翻页
//            pageNo++;
//        }
//    }
}
