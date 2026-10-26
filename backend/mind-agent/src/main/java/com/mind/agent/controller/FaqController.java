package com.mind.agent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mind.agent.entity.DialogMessage;
import com.mind.agent.entity.Faq;
import com.mind.agent.entity.FaqFeedback;
import com.mind.agent.mapper.DialogMessageMapper;
import com.mind.agent.mapper.FaqFeedbackMapper;
import com.mind.agent.mapper.FaqMapper;
import com.mind.common.Result;
import com.mind.common.ai.AiServiceClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 高频问题：从学生追问记录自动聚合提问热度，教师发布标准解答，
 * 学生提问时优先命中已发布解答（见 AgentController 的前置匹配）。
 */
@RestController
@RequestMapping("/api/agent/faq")
public class FaqController {

    private final FaqMapper faqMapper;
    private final FaqFeedbackMapper feedbackMapper;
    private final DialogMessageMapper messageMapper;
    private final AiServiceClient aiClient;

    public FaqController(FaqMapper faqMapper, FaqFeedbackMapper feedbackMapper,
                         DialogMessageMapper messageMapper, AiServiceClient aiClient) {
        this.faqMapper = faqMapper;
        this.feedbackMapper = feedbackMapper;
        this.messageMapper = messageMapper;
        this.aiClient = aiClient;
    }

    /** 学生端：高频问题榜（先聚合刷新，再返回；已发布的才带解答） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        aggregate();
        QueryWrapper<Faq> qw = new QueryWrapper<Faq>().ge("ask_count", 1).orderByDesc("ask_count").last("LIMIT 50");
        if (keyword != null && !keyword.isBlank()) qw.like("question_text", keyword);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Faq f : faqMapper.selectList(qw)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("questionText", f.getQuestionText());
            m.put("conceptName", f.getConceptName());
            m.put("askCount", f.getAskCount());
            m.put("answered", f.getPublishStatus() != null && f.getPublishStatus() == 1);
            m.put("answer", f.getPublishStatus() != null && f.getPublishStatus() == 1 ? f.getAnswer() : null);
            Map<String, Object> fb = feedbackStats(f.getId());
            m.put("useful", fb.get("useful"));
            m.put("useless", fb.get("useless"));
            Long me = StpUtil.getLoginIdAsLong();
            FaqFeedback mine = feedbackMapper.selectOne(new QueryWrapper<FaqFeedback>()
                    .eq("faq_id", f.getId()).eq("student_id", me).last("LIMIT 1"));
            m.put("myFeedback", mine == null ? null : (mine.getUseful() == 1 ? "useful" : "useless"));
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 学生端：反馈解答有用/无用（一人一条，可改） */
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        boolean useful = !"useless".equals(String.valueOf(body.get("feedback")));
        Long me = StpUtil.getLoginIdAsLong();
        FaqFeedback fb = feedbackMapper.selectOne(new QueryWrapper<FaqFeedback>()
                .eq("faq_id", id).eq("student_id", me).last("LIMIT 1"));
        if (fb == null) {
            fb = new FaqFeedback();
            fb.setFaqId(id);
            fb.setStudentId(me);
            fb.setUseful(useful ? 1 : 0);
            fb.setCreatedAt(LocalDateTime.now());
            feedbackMapper.insert(fb);
        } else {
            fb.setUseful(useful ? 1 : 0);
            feedbackMapper.updateById(fb);
        }
        return Result.ok();
    }

    /** 教师端：高频问题工作台（含待解答队列） */
    @GetMapping("/manage")
    public Result<Map<String, Object>> manage() {
        aggregate();
        List<Faq> all = faqMapper.selectList(new QueryWrapper<Faq>().orderByDesc("ask_count").last("LIMIT 100"));
        List<Map<String, Object>> items = new ArrayList<>();
        int pending = 0;
        for (Faq f : all) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("questionText", f.getQuestionText());
            m.put("conceptName", f.getConceptName());
            m.put("askCount", f.getAskCount());
            m.put("answer", f.getAnswer());
            m.put("publishStatus", f.getPublishStatus());
            Map<String, Object> fb = feedbackStats(f.getId());
            m.put("useful", fb.get("useful"));
            m.put("useless", fb.get("useless"));
            items.add(m);
            if (f.getPublishStatus() == null || f.getPublishStatus() != 1) pending++;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("pending", pending);
        return Result.ok(data);
    }

    /** 教师端：发布 / 更新标准解答 */
    @PostMapping("/answer")
    public Result<Void> answer(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String answer = String.valueOf(body.get("answer"));
        Faq f = faqMapper.selectById(id);
        if (f == null) return Result.error("问题不存在");
        faqMapper.update(null, new UpdateWrapper<Faq>()
                .eq("id", id)
                .set("answer", answer)
                .set("publish_status", 1)
                .set("answered_by", StpUtil.getLoginIdAsLong())
                .set("answered_at", LocalDateTime.now()));
        return Result.ok();
    }

    /**
     * 教师端：智能归并——LLM 把语义相同的问题分组，组内合并到提问最多的一条
     * （提问次数累加、反馈记录迁移），减少重复解答。
     * LLM 不可用或输出不可信时不改动任何数据。
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/cluster")
    public Result<Map<String, Object>> cluster() {
        aggregate();
        List<Faq> candidates = faqMapper.selectList(
                new QueryWrapper<Faq>().orderByDesc("ask_count").last("LIMIT 50"));
        if (candidates.size() < 2) return Result.error("问题太少，无需归并");

        List<String> questions = new ArrayList<>();
        for (Faq f : candidates) questions.add(f.getQuestionText());

        Map<String, Object> resp;
        try {
            resp = aiClient.clusterQuestions(questions);
        } catch (Exception e) {
            return Result.error("智能归并调用失败: " + e.getMessage());
        }
        Object ok = resp.get("ok");
        if (!(ok instanceof Boolean && (Boolean) ok)) {
            return Result.error("大模型暂不可用，未执行归并（问题列表未改动）");
        }
        List<List<Integer>> groups;
        try {
            groups = (List<List<Integer>>) resp.get("groups");
        } catch (Exception e) {
            return Result.error("归并结果解析失败，未改动数据");
        }

        int merged = 0;
        for (List<Integer> g : groups) {
            if (g == null || g.size() < 2) continue;
            // 归并基准：优先保留"已发布且有解答"的一条；其次提问次数最多的一条（代表文本保留，次数累加）
            Faq canonical = null;
            for (int idx : g) {
                Faq f = candidates.get(idx);
                if (canonical == null) { canonical = f; continue; }
                boolean fPub = f.getPublishStatus() != null && f.getPublishStatus() == 1
                        && f.getAnswer() != null && !f.getAnswer().isBlank();
                boolean cPub = canonical.getPublishStatus() != null && canonical.getPublishStatus() == 1
                        && canonical.getAnswer() != null && !canonical.getAnswer().isBlank();
                if (fPub != cPub) { if (fPub) canonical = f; continue; }
                int fc = f.getAskCount() == null ? 0 : f.getAskCount();
                int cc = canonical.getAskCount() == null ? 0 : canonical.getAskCount();
                if (fc > cc) canonical = f;
            }
            // 解答继承：基准没有解答而组内其他有，带过来，避免丢教师劳动成果
            if (canonical.getAnswer() == null || canonical.getAnswer().isBlank()) {
                for (int idx : g) {
                    Faq f = candidates.get(idx);
                    if (f.getId().equals(canonical.getId())) continue;
                    if (f.getAnswer() != null && !f.getAnswer().isBlank()) {
                        canonical.setAnswer(f.getAnswer());
                        canonical.setPublishStatus(1);
                        break;
                    }
                }
            }
            int sum = 0;
            for (int idx : g) {
                Faq f = candidates.get(idx);
                sum += f.getAskCount() == null ? 0 : f.getAskCount();
                if (f.getId().equals(canonical.getId())) continue;
                feedbackMapper.update(null, new UpdateWrapper<FaqFeedback>()
                        .eq("faq_id", f.getId()).set("faq_id", canonical.getId()));
                faqMapper.deleteById(f.getId());
                merged++;
            }
            canonical.setAskCount(sum);
            faqMapper.updateById(canonical);
        }
        return Result.ok(Map.of("merged", merged));
    }

    /** 聚合学生追问记录为高频问题（按规范化文本分组，按提问人数计数） */
    private void aggregate() {
        List<DialogMessage> userMsgs = messageMapper.selectList(new QueryWrapper<DialogMessage>()
                .eq("role", "USER").orderByDesc("id").last("LIMIT 500"));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DialogMessage m : userMsgs) {
            String q = normalize(m.getContent());
            if (q.length() < 4) continue;
            counts.merge(q, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Faq exist = faqMapper.selectOne(new QueryWrapper<Faq>()
                    .eq("question_text", e.getKey()).last("LIMIT 1"));
            if (exist == null) {
                Faq f = new Faq();
                f.setQuestionText(e.getKey());
                f.setAskCount(e.getValue());
                f.setPublishStatus(0);
                f.setCreatedAt(LocalDateTime.now());
                faqMapper.insert(f);
            } else if (e.getValue() > exist.getAskCount()) {
                exist.setAskCount(e.getValue());
                faqMapper.updateById(exist);
            }
        }
    }

    private Map<String, Object> feedbackStats(Long faqId) {
        Long useful = feedbackMapper.selectCount(new QueryWrapper<FaqFeedback>()
                .eq("faq_id", faqId).eq("useful", 1));
        Long useless = feedbackMapper.selectCount(new QueryWrapper<FaqFeedback>()
                .eq("faq_id", faqId).eq("useful", 0));
        Map<String, Object> m = new HashMap<>();
        m.put("useful", useful == null ? 0 : useful.intValue());
        m.put("useless", useless == null ? 0 : useless.intValue());
        return m;
    }

    /** 提问文本规范化：去空白和标点，用于聚合同义提问 */
    static String normalize(String content) {
        if (content == null) return "";
        String q = content.trim();
        if (q.length() > 60) q = q.substring(0, 60);
        return q.replaceAll("[\\s，。？！?!.,;；：:]", "");
    }
}
