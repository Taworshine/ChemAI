package com.mind.agent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.agent.entity.DialogMessage;
import com.mind.agent.entity.DialogSession;
import com.mind.agent.mapper.DialogMessageMapper;
import com.mind.agent.mapper.DialogSessionMapper;
import com.mind.common.Result;
import com.mind.common.ai.AiServiceClient;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 智能对话（学生端）：会话管理 + SSE 流式追问
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final DialogSessionMapper sessionMapper;
    private final DialogMessageMapper messageMapper;
    private final AiServiceClient aiClient;
    private final com.mind.agent.mapper.FaqMapper faqMapper;
    private final Driver driver;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentController(DialogSessionMapper sessionMapper, DialogMessageMapper messageMapper,
                           AiServiceClient aiClient, com.mind.agent.mapper.FaqMapper faqMapper, Driver driver) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.aiClient = aiClient;
        this.faqMapper = faqMapper;
        this.driver = driver;
    }

    /** 创建会话 */
    @PostMapping("/session")
    public Result<DialogSession> createSession(@RequestBody(required = false) Map<String, Object> body) {
        DialogSession session = new DialogSession();
        session.setStudentId(StpUtil.getLoginIdAsLong());
        if (body != null && body.get("questionTypeId") != null) {
            session.setQuestionTypeId(Long.valueOf(String.valueOf(body.get("questionTypeId"))));
        }
        session.setStatus(0);
        sessionMapper.insert(session);
        return Result.ok(session);
    }

    /** 当前学生的会话列表 */
    @GetMapping("/sessions")
    public Result<List<DialogSession>> sessions() {
        return Result.ok(sessionMapper.selectList(
                new QueryWrapper<DialogSession>().eq("student_id", StpUtil.getLoginIdAsLong()).orderByDesc("id")));
    }

    /** 会话历史消息 */
    @GetMapping("/history/{id}")
    public Result<List<DialogMessage>> history(@PathVariable("id") Long id) {
        return Result.ok(messageMapper.selectList(
                new QueryWrapper<DialogMessage>().eq("session_id", id).orderByAsc("id")));
    }

    /** 关闭 / 标记解决 */
    @PutMapping("/session/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Integer> body) {
        sessionMapper.update(null, new UpdateWrapper<DialogSession>().eq("id", id).set("status", body.get("status")));
        return Result.ok();
    }

    /**
     * 发送消息，SSE 流式返回 AI 追问。
     * 流程：保存学生消息 → 组装思维路径上下文 → 调用 Python AI 服务 → 流式推送 → 落库
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(String.valueOf(body.get("sessionId")));
        String message = String.valueOf(body.get("message"));

        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                // 1. 保存学生消息
                DialogMessage userMsg = new DialogMessage();
                userMsg.setSessionId(sessionId);
                userMsg.setRole("USER");
                userMsg.setContent(message);
                messageMapper.insert(userMsg);

                // 1.5 立即推送"思考中"：LLM（尤其 reasoning 模型）首token前可能要等几十秒，
                // 先让前端出现气泡，避免"卡死"的观感
                emitter.send(SseEmitter.event().name("thinking").data("thinking"));

                // 2. 组装上下文：该题型在 Neo4j 中的思维路径 + 历史消息
                DialogSession session = sessionMapper.selectById(sessionId);
                Map<String, Object> context = buildContext(session);
                List<Map<String, Object>> history = new ArrayList<>();
                List<DialogMessage> msgs = messageMapper.selectList(
                        new QueryWrapper<DialogMessage>().eq("session_id", sessionId).orderByAsc("id"));
                // history 不含当前这条消息（当前消息由 message 参数单独传递），
                // 否则引擎会把首问误判为"已推进"
                for (int i = 0; i < msgs.size() - 1; i++) {
                    DialogMessage m = msgs.get(i);
                    Map<String, Object> item = new HashMap<>();
                    item.put("role", "user".equalsIgnoreCase(m.getRole()) ? "user" : "assistant");
                    item.put("content", m.getContent());
                    history.add(item);
                }

                // 3. 高频问题优先命中：已发布标准解答直接回复，不再调用大模型
                com.mind.agent.entity.Faq hitFaq = matchFaq(message);
                String reply;
                String misconceptionType;
                int nextStep;
                if (hitFaq != null) {
                    hitFaq.setAskCount(hitFaq.getAskCount() + 1);
                    faqMapper.updateById(hitFaq);
                    reply = hitFaq.getAnswer() + "\n\n如果还有不清楚的地方，可以继续问我～";
                    misconceptionType = null;
                    List<DialogMessage> lastAi = messageMapper.selectList(new QueryWrapper<DialogMessage>()
                            .eq("session_id", sessionId).eq("role", "AI")
                            .isNotNull("thinking_step").orderByDesc("id").last("LIMIT 1"));
                    nextStep = lastAi.isEmpty() || lastAi.get(0).getThinkingStep() == null
                            ? 0 : lastAi.get(0).getThinkingStep();
                    // 标准解答是现成的，快进式分段推送（每段 3 字、15ms），既丝滑又不拖时间
                    for (int i = 0; i < reply.length(); i += 3) {
                        emitter.send(SseEmitter.event().name("message")
                                .data(reply.substring(i, Math.min(i + 3, reply.length()))));
                        Thread.sleep(15);
                    }
                } else {
                    // 4. 调用 Python AI 服务流式生成追问，逐段透传给前端
                    StringBuilder sb = new StringBuilder();
                    Map<String, Object>[] resultHolder = new Map[1];
                    aiClient.chatStream(message, history, context, new AiServiceClient.ChatStreamHandler() {
                        @Override
                        public void onDelta(String text) throws Exception {
                            sb.append(text);
                            emitter.send(SseEmitter.event().name("message").data(text));
                        }

                        @Override
                        public void onResult(Map<String, Object> result) {
                            resultHolder[0] = result;
                        }
                    });
                    Map<String, Object> resp = resultHolder[0];
                    if (resp == null) {
                        throw new RuntimeException("AI 服务未返回结果");
                    }
                    reply = String.valueOf(resp.get("reply"));
                    misconceptionType = resp.get("misconception_type") == null ? null : String.valueOf(resp.get("misconception_type"));
                    nextStep = resp.get("next_step") == null ? 0 : Integer.parseInt(String.valueOf(resp.get("next_step")));
                }

                emitter.send(SseEmitter.event().name("meta").data(
                        "{\"misconceptionType\":\"" + (misconceptionType == null ? "" : misconceptionType)
                                + "\",\"nextStep\":" + nextStep + "}"));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));

                // 5. 落库 AI 消息 + 更新画像/会话总结
                DialogMessage aiMsg = new DialogMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setRole("AI");
                aiMsg.setContent(reply);
                aiMsg.setMisconceptionType(misconceptionType);
                aiMsg.setThinkingStep(nextStep);
                messageMapper.insert(aiMsg);

                if (misconceptionType != null) {
                    // 记录卡点到会话总结，供教师端学情分析
                    sessionMapper.update(null, new UpdateWrapper<DialogSession>()
                            .eq("id", sessionId).set("summary", "检测到卡点：" + misconceptionType));
                }
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务异常：" + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 与已发布的高频问题做 2-gram 相似度匹配，≥0.35 即命中标准解答 */
    private com.mind.agent.entity.Faq matchFaq(String message) {
        List<com.mind.agent.entity.Faq> published = faqMapper.selectList(new QueryWrapper<com.mind.agent.entity.Faq>()
                .eq("publish_status", 1).orderByDesc("ask_count").last("LIMIT 50"));
        if (published.isEmpty()) return null;
        Set<String> grams = bigrams(FaqController.normalize(message));
        if (grams.isEmpty()) return null;
        com.mind.agent.entity.Faq best = null;
        double bestScore = 0;
        for (com.mind.agent.entity.Faq f : published) {
            Set<String> fg = bigrams(FaqController.normalize(f.getQuestionText()));
            if (fg.isEmpty()) continue;
            Set<String> inter = new HashSet<>(grams);
            inter.retainAll(fg);
            double score = (double) inter.size() / (grams.size() + fg.size() - inter.size());
            if (score > bestScore) {
                bestScore = score;
                best = f;
            }
        }
        return bestScore >= 0.35 ? best : null;
    }

    private static Set<String> bigrams(String s) {
        Set<String> out = new HashSet<>();
        for (int i = 0; i + 2 <= s.length(); i++) out.add(s.substring(i, i + 2));
        return out;
    }

    /** 从 Neo4j 拉取该题型的思维路径作为追问上下文（含每步提示 hint） */
    private Map<String, Object> buildContext(DialogSession session) {
        Map<String, Object> context = new HashMap<>();
        List<String> path = new ArrayList<>();
        List<String> hints = new ArrayList<>();
        String misconception = null;
        int currentStep = 0;
        if (session != null && session.getQuestionTypeId() != null) {
            try (Session s = driver.session()) {
                s.run("MATCH (q:QuestionType {id: $id})-[:REQUIRES_PATH]->(p:ThinkingPath) " +
                                "RETURN p ORDER BY p.stepOrder",
                        org.neo4j.driver.Values.parameters("id", session.getQuestionTypeId()))
                        .forEachRemaining(r -> {
                            path.add(r.get("p").get("description").asString());
                            hints.add(r.get("p").get("hint").isNull() ? "" : r.get("p").get("hint").asString());
                        });
            } catch (Exception ignored) {
            }
        }
        // 最近一次检测到的卡点 + 当前思维步骤
        List<DialogMessage> recent = messageMapper.selectList(new QueryWrapper<DialogMessage>()
                .eq("session_id", session.getId()).isNotNull("misconception_type")
                .orderByDesc("id").last("LIMIT 1"));
        if (!recent.isEmpty()) {
            misconception = recent.get(0).getMisconceptionType();
        }
        List<DialogMessage> lastAi = messageMapper.selectList(new QueryWrapper<DialogMessage>()
                .eq("session_id", session.getId()).eq("role", "AI")
                .isNotNull("thinking_step").orderByDesc("id").last("LIMIT 1"));
        if (!lastAi.isEmpty() && lastAi.get(0).getThinkingStep() != null) {
            currentStep = lastAi.get(0).getThinkingStep();
        }
        context.put("path", path);
        context.put("hints", hints);
        context.put("current_step", currentStep);
        context.put("misconception", misconception);
        return context;
    }
}
