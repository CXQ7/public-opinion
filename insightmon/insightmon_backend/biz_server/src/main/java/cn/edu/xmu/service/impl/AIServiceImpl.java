package cn.edu.xmu.service.impl;

import cn.edu.xmu.service.AIService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.*;

@Service
@Slf4j
public class AIServiceImpl implements AIService {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static final String API_KEY = "__CONFIGURE_LOCALLY__";
    public static final String SECRET_KEY = "__CONFIGURE_LOCALLY__";

    @Override
    public String ask(String queryContent) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            String reply;
            try {
                reply = get_total_reply(queryContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return reply;
        }, executorService);

        // 这里可以做一些其他的事情，而不需要等待结果

        try {
            // 等待异步任务完成并获取结果
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            // 处理异常情况
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build();


    // 获取完整答案后，一次性输出
    public static String get_total_reply(String question) throws IOException{
        MediaType mediaType = MediaType.parse("application/json");
        String requestContent = new StringBuffer()
                .append("{\"messages\":[{\"role\":\"user\",\"content\":\"")
                .append(question)
                .append("\"}],\"temperature\":0.95,\"top_p\":0.8,\"penalty_score\":1,\"enable_system_memory\":false,\"disable_search\":false,\"enable_citation\":false,\"response_format\":\"text\"}")
                .toString();
        System.out.println("requestContent: " + requestContent);
        RequestBody body = RequestBody.create(mediaType, requestContent);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        JSONObject resObj = new JSONObject(response.body().string());
        log.info("GPT返回结果: {}", resObj.getString("result"));
        return resObj.getString("result");
    }


    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    static String getAccessToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY
                + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String str = response.body().string();
        JSONObject obj = new JSONObject(str);
        return obj.getString("access_token");
    }

    public static void main(String[] args) {
        AIServiceImpl aiService = new AIServiceImpl();
        String reply = aiService.ask("请简要介绍一下人工智能的发展历史。");
        System.out.println("AI回复: " + reply);
    }
}
