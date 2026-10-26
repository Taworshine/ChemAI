package com.mind.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.agent.entity.DialogMessage;
import com.mind.agent.entity.DialogSession;
import com.mind.agent.entity.Faq;
import com.mind.agent.mapper.DialogMessageMapper;
import com.mind.agent.mapper.DialogSessionMapper;
import com.mind.agent.mapper.FaqMapper;
import com.mind.agent.mapper.TaskStatMapper;
import com.mind.common.Result;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 教研看板：图谱资产 + 任务进度 + 卡点热点 + 缺口清单（学生常问但图谱没有的知识点）。
 */
@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final Driver driver;
    private final DialogMessageMapper messageMapper;
    private final DialogSessionMapper sessionMapper;
    private final FaqMapper faqMapper;
    private final TaskStatMapper taskStatMapper;

    public ResearchController(Driver driver, DialogMessageMapper messageMapper,
                              DialogSessionMapper sessionMapper, FaqMapper faqMapper,
                              TaskStatMapper taskStatMapper) {
        this.driver = driver;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.faqMapper = faqMapper;
        this.taskStatMapper = taskStatMapper;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        try (Session s = driver.session()) {
            // 图谱资产：按标签统计 + 草稿/已发布
            Map<String, Integer> byLabel = new LinkedHashMap<>();
            s.run("MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                    "RETURN labels(n)[0] AS label, count(*) AS c ORDER BY c DESC")
                    .forEachRemaining(r -> byLabel.put(r.get("label").asString(), r.get("c").asInt()));
            data.put("byLabel", byLabel);

            Map<String, Integer> byCuration = new LinkedHashMap<>();
            s.run("MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                    "RETURN coalesce(n.curation_status, '种子数据') AS st, count(*) AS c")
                    .forEachRemaining(r -> byCuration.put(r.get("st").asString(), r.get("c").asInt()));
            data.put("byCuration", byCuration);

            // 近 30 天新增（入库节点带 created_at）
            Integer recent = s.run("MATCH (n) WHERE n.created_at IS NOT NULL AND n.created_at > datetime() - duration('P30D') RETURN count(*) AS c")
                    .single().get("c").asInt();
            data.put("recent30", recent);

            // 学科覆盖
            Map<String, Integer> bySubject = new LinkedHashMap<>();
            s.run("MATCH (c:Concept) RETURN coalesce(c.subject, '未标注') AS subj, count(*) AS c ORDER BY c DESC")
                    .forEachRemaining(r -> bySubject.put(r.get("subj").asString(), r.get("c").asInt()));
            data.put("bySubject", bySubject);
        }

        // 入库任务状态
        data.put("taskStatus", taskStatMapper.statusCounts());
        data.put("pendingItems", taskStatMapper.pendingItems() == null ? 0 : taskStatMapper.pendingItems());

        // 卡点热点 Top5
        List<Map<String, Object>> misconceptionTop = new ArrayList<>();
        messageMapper.selectMaps(new QueryWrapper<DialogMessage>()
                        .select("misconception_type AS type", "COUNT(*) AS c")
                        .isNotNull("misconception_type")
                        .ne("misconception_type", "")
                        .groupBy("misconception_type")
                        .orderByDesc("c")
                        .last("LIMIT 5"))
                .forEach(m -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", String.valueOf(m.get("type")));
                    item.put("count", ((Number) m.get("c")).intValue());
                    misconceptionTop.add(item);
                });
        data.put("misconceptionTop", misconceptionTop);
        data.put("sessionCount", sessionMapper.selectCount(null));
        data.put("faqPending", faqMapper.selectCount(new QueryWrapper<Faq>().ne("publish_status", 1)));
        return Result.ok(data);
    }

    /**
     * 缺口清单：学生高频提问（≥2 人问）但图谱中没有任何概念名能覆盖 → 建议补知识点。
     * 教师可一键转为 C 类上传任务。
     */
    @GetMapping("/gap-list")
    public Result<List<Map<String, Object>>> gapList() {
        Set<String> conceptNames = new HashSet<>();
        try (Session s = driver.session()) {
            s.run("MATCH (n) WHERE n:Concept OR n:Entity RETURN n.name AS name")
                    .forEachRemaining(r -> {
                        if (!r.get("name").isNull()) conceptNames.add(r.get("name").asString());
                    });
        }
        List<Map<String, Object>> gaps = new ArrayList<>();
        for (Faq f : faqMapper.selectList(new QueryWrapper<Faq>()
                .ge("ask_count", 2).orderByDesc("ask_count").last("LIMIT 50"))) {
            String q = f.getQuestionText();
            boolean covered = false;
            for (String name : conceptNames) {
                if (name != null && !name.isBlank() && q.contains(name)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", f.getId());
                m.put("question", q);
                m.put("askCount", f.getAskCount());
                m.put("answered", f.getPublishStatus() != null && f.getPublishStatus() == 1);
                gaps.add(m);
            }
        }
        return Result.ok(gaps);
    }
}
