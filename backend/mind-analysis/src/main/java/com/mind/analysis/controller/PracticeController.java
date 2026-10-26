package com.mind.analysis.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.analysis.entity.PracticeRecord;
import com.mind.analysis.mapper.PracticeRecordMapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.QuestionBankMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线练习：按学科随机抽题 → 作答 → 自动判分 → 记录归档
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final QuestionBankMapper questionBankMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PracticeController(QuestionBankMapper questionBankMapper, PracticeRecordMapper practiceRecordMapper) {
        this.questionBankMapper = questionBankMapper;
        this.practiceRecordMapper = practiceRecordMapper;
    }

    /** 随机抽题（不返回答案） */
    @GetMapping("/questions")
    public Result<List<Map<String, Object>>> questions(@RequestParam String subject,
                                                       @RequestParam(defaultValue = "5") int count) {
        List<QuestionBank> all = questionBankMapper.selectList(
                new QueryWrapper<QuestionBank>().eq("subject", subject));
        Collections.shuffle(all);
        List<Map<String, Object>> result = all.stream().limit(count).map(q -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("subject", q.getSubject());
            m.put("conceptName", q.getConceptName());
            m.put("questionType", q.getQuestionType());
            m.put("content", q.getContent());
            m.put("difficulty", q.getDifficulty());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    /** 提交作答：自动判分并归档 */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) throws Exception {
        Long studentId = StpUtil.getLoginIdAsLong();
        String subject = String.valueOf(body.get("subject"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");

        int correct = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map<String, Object> a : answers) {
            Long qid = Long.valueOf(String.valueOf(a.get("questionId")));
            String userAnswer = a.get("userAnswer") == null ? "" : String.valueOf(a.get("userAnswer"));
            QuestionBank q = questionBankMapper.selectById(qid);
            if (q == null) continue;
            boolean right = normalize(userAnswer).equals(normalize(q.getAnswer()));
            if (right) correct++;
            Map<String, Object> d = new HashMap<>();
            d.put("questionId", qid);
            d.put("content", q.getContent());
            d.put("userAnswer", userAnswer);
            d.put("rightAnswer", q.getAnswer());
            d.put("correct", right);
            details.add(d);
        }

        PracticeRecord record = new PracticeRecord();
        record.setStudentId(studentId);
        record.setSubject(subject);
        record.setScore(correct);
        record.setTotal(details.size());
        record.setDetail(objectMapper.writeValueAsString(details));
        practiceRecordMapper.insert(record);

        Map<String, Object> data = new HashMap<>();
        data.put("score", correct);
        data.put("total", details.size());
        data.put("details", details);
        return Result.ok(data);
    }

    /** 我的练习记录（学生端） */
    @GetMapping("/records")
    public Result<List<PracticeRecord>> records() {
        return Result.ok(practiceRecordMapper.selectList(
                new QueryWrapper<PracticeRecord>().eq("student_id", StpUtil.getLoginIdAsLong())
                        .orderByDesc("id")));
    }

    /** 练习统计（教师端看板）：各学科平均分 + 最近记录 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<PracticeRecord> all = practiceRecordMapper.selectList(new QueryWrapper<>());
        Map<String, List<PracticeRecord>> bySubject = all.stream()
                .filter(r -> r.getTotal() != null && r.getTotal() > 0)
                .collect(Collectors.groupingBy(PracticeRecord::getSubject));
        Map<String, Object> data = new HashMap<>();
        data.put("bySubject", bySubject.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            double avg = e.getValue().stream().mapToInt(r -> r.getScore() * 100 / r.getTotal()).average().orElse(0);
            m.put("subject", e.getKey());
            m.put("avg", Math.round(avg));
            m.put("count", e.getValue().size());
            return m;
        }).collect(Collectors.toList()));
        data.put("recent", all.stream().sorted(Comparator.comparing(PracticeRecord::getId).reversed())
                .limit(8).collect(Collectors.toList()));
        return Result.ok(data);
    }

    /** 判分归一化：去空白、统一全角半角括号与标点 */
    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "")
                .replace('（', '(').replace('）', ')')
                .replace('，', ',').replace('。', '.')
                .replace("↑", "").toLowerCase();
    }
}
