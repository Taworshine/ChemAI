package com.mind.mobile.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.QuestionBankMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 命题研讨：题目查重（同概念检测）/ 智能组卷 / 试卷导出
 */
@RestController
@RequestMapping("/api/mobile/question")
public class QuestionController {

    private final QuestionBankMapper questionBankMapper;

    public QuestionController(QuestionBankMapper questionBankMapper) {
        this.questionBankMapper = questionBankMapper;
    }

    /**
     * 题目查重：按概念名在题库中找疑似重复题
     * 进阶：与 Neo4j 概念节点比对，判断该题考查的知识点在题库中的覆盖密度
     */
    @PostMapping("/duplicate-check")
    public Result<Map<String, Object>> duplicateCheck(@RequestBody Map<String, String> body) {
        String conceptName = body.get("conceptName");
        String content = body.get("content");
        List<Map<String, Object>> duplicates = new ArrayList<>();

        if (conceptName != null && !conceptName.isEmpty()) {
            questionBankMapper.selectList(new QueryWrapper<QuestionBank>()
                            .eq("concept_name", conceptName)).forEach(q -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", q.getId());
                item.put("content", q.getContent());
                item.put("reason", "同知识点：" + conceptName);
                duplicates.add(item);
            });
        }
        if (content != null && content.length() >= 8) {
            String key = content.substring(0, Math.min(8, content.length()));
            questionBankMapper.selectList(new QueryWrapper<QuestionBank>()
                    .like("content", key)).forEach(q -> {
                boolean exists = duplicates.stream().anyMatch(d -> d.get("id").equals(q.getId()));
                if (!exists) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", q.getId());
                    item.put("content", q.getContent());
                    item.put("reason", "题干前 8 字相同");
                    duplicates.add(item);
                }
            });
        }

        long conceptCount = conceptName == null ? 0 : questionBankMapper.selectCount(
                new QueryWrapper<QuestionBank>().eq("concept_name", conceptName));
        Map<String, Object> data = new HashMap<>();
        data.put("duplicates", duplicates);
        data.put("conceptQuestionCount", conceptCount);
        data.put("warning", conceptCount >= 3 ? "该知识点已有 " + conceptCount + " 道题，建议换个角度命题" : "");
        return Result.ok(data);
    }

    /**
     * 智能组卷：按知识点分布 + 难度比例抽题
     * body: {subject, count(总题数), easy/medium/hard(各难度数量,可选)}
     */
    @PostMapping("/paper/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String subject = String.valueOf(body.get("subject"));
        int count = body.get("count") == null ? 10
                : Integer.parseInt(String.valueOf(body.get("count")));
        List<QuestionBank> pool = questionBankMapper.selectList(
                new QueryWrapper<QuestionBank>().eq("subject", subject));

        // 按知识点均匀抽取：每个概念先各取 1 题，不足再循环补齐
        Map<String, List<QuestionBank>> byConcept = pool.stream()
                .collect(Collectors.groupingBy(q -> q.getConceptName() == null ? "未标注" : q.getConceptName(),
                        LinkedHashMap::new, Collectors.toList()));
        byConcept.values().forEach(list -> Collections.shuffle(list));

        List<QuestionBank> paper = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        List<String> conceptOrder = new ArrayList<>(byConcept.keySet());
        int round = 0;
        while (paper.size() < count && !conceptOrder.isEmpty()) {
            boolean added = false;
            for (String concept : conceptOrder) {
                List<QuestionBank> list = byConcept.get(concept);
                if (list.size() > round && paper.size() < count) {
                    QuestionBank q = list.get(round);
                    if (used.add(q.getId())) {
                        paper.add(q);
                        added = true;
                    }
                }
            }
            if (!added) {
                break;
            }
            round++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("subject", subject);
        data.put("questions", paper);
        data.put("concepts", paper.stream().map(QuestionBank::getConceptName).distinct()
                .collect(Collectors.toList()));
        return Result.ok(data);
    }

    /** 试卷导出：生成纯文本试卷（题目+留空），下载为 .txt */
    @PostMapping("/paper/export")
    public ResponseEntity<byte[]> export(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(body.getOrDefault("title", "教研组试卷"))).append("\n\n");
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                sb.append(i + 1).append(". ")
                        .append(q.get("content")).append("（").append(q.get("difficulty")).append("）\n\n");
            }
            sb.append("——参考答案——\n\n");
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                sb.append(i + 1).append(". ").append(q.get("answer")).append("\n");
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "paper.txt");
        return ResponseEntity.ok().headers(headers)
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
