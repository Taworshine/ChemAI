package com.mind.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java 调用 Python AI 服务（FastAPI, 端口 8000）的 HTTP 客户端。
 * LLM 调用较慢，超时统一设置为 60s。
 */
@Component
public class AiServiceClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(@Value("${ai.service.url:http://localhost:8000}") String baseUrl) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(180000); // LLM 抽取较慢（免费档限流重试），3 分钟上限
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 文档知识抽取
     *
     * @param docId    文档 ID
     * @param filePath 本地文件路径（Python 服务与本机同机部署，可直接读取）
     * @param text     若已提取文本则直接传文本
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extract(Long docId, String filePath, String text) {
        return extract(docId, "A", filePath, text);
    }

    /**
     * 按文档类型抽取（A=教案 B=讲义 C=纯知识点），输出带 confidence/quote 的结构化结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extract(Long docId, String docType, String filePath, String text) {
        Map<String, Object> req = new HashMap<>();
        req.put("doc_id", docId);
        req.put("doc_type", docType == null ? "A" : docType);
        req.put("file_path", filePath);
        req.put("text", text);
        return postForMap("/api/extract", req);
    }

    /**
     * 启发式追问生成
     */
    public Map<String, Object> chat(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        Map<String, Object> req = new HashMap<>();
        req.put("message", message);
        req.put("history", history);
        req.put("context", context);
        return postForMap("/api/chat", req);
    }

    /** 流式追问回调：onDelta 收逐段文本，onResult 收最终结果（含 reply/misconception_type/next_step/engine） */
    public interface ChatStreamHandler {
        void onDelta(String text) throws Exception;
        void onResult(Map<String, Object> result);
    }

    /**
     * 流式启发式追问：直连 Python /api/chat/stream（SSE），逐段回调。
     * RestTemplate 会缓冲整个响应，流式必须走 java.net.http.HttpClient 的 ofLines。
     */
    public void chatStream(String message, List<Map<String, Object>> history,
                           Map<String, Object> context, ChatStreamHandler handler) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("message", message);
            req.put("history", history);
            req.put("context", context);
            // 必须显式 HTTP/1.1：默认 HTTP/2(h2c 升级)会让 uvicorn 收不到请求体（422）
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat/stream"))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(req),
                            java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            HttpResponse<java.util.stream.Stream<String>> resp =
                    client.send(request, HttpResponse.BodyHandlers.ofLines());
            // 必须惰性逐行消费（不可 toList()）：否则要等 Python 全部生成完才开始转发，失去流式意义
            String event = null;
            for (String line : (Iterable<String>) resp.body()::iterator) {
                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    String data = line.substring("data:".length()).trim();
                    if ("delta".equals(event)) {
                        handler.onDelta(objectMapper.readTree(data).get("text").asText());
                    } else if ("result".equals(event)) {
                        handler.onResult(objectMapper.readValue(data, Map.class));
                    }
                    event = null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("AI 流式调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 高频问题语义聚类：LLM 把同义问题归组，返回 [[索引...], ...]。
     * ok=false 表示 LLM 不可用/输出不可信，调用方应放弃合并。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> clusterQuestions(List<String> questions) {
        Map<String, Object> req = new HashMap<>();
        req.put("questions", questions);
        return postForMap("/api/faq/cluster", req);
    }

    public boolean health() {
        try {
            String body = restTemplate.getForObject(baseUrl + "/api/health", String.class);
            return body != null && body.contains("\"ok\":true");
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> postForMap(String path, Map<String, Object> req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
        String body = restTemplate.postForObject(baseUrl + path, entity, String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("ok") && !node.get("ok").asBoolean()) {
                throw new RuntimeException("AI 服务返回错误: " + body);
            }
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 服务响应解析失败: " + body, e);
        }
    }
}
