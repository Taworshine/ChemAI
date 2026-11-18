package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileWrongQuestion;
import com.mind.mobile.mapper.MobileWrongQuestionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 错题本：录入 / 列表 / 薄弱点聚合（供学情洞察） */
@RestController
@RequestMapping("/api/mobile/wrong")
public class WrongQuestionController {

    private final MobileWrongQuestionMapper wrongMapper;
    private final com.mind.system.service.SubjectContext subjectContext;

    public WrongQuestionController(MobileWrongQuestionMapper wrongMapper,
                                   com.mind.system.service.SubjectContext subjectContext) {
        this.wrongMapper = wrongMapper;
        this.subjectContext = subjectContext;
    }

    /** 录入错题（AI 识别在前端/解析服务完成后提交确认结果） */
    @PostMapping("/add")
    public Result<MobileWrongQuestion> add(@RequestBody MobileWrongQuestion wq) {
        wq.setId(null);
        wq.setCreateBy(StpUtil.getLoginIdAsLong());
        wq.setCreateTime(LocalDateTime.now());
        // 学科强制继承录入教师的所属学科，客户端不可伪造
        wq.setSubject(subjectContext.currentSubject());
        if (wq.getWrongCount() == null) wq.setWrongCount(1);
        wrongMapper.insert(wq);
        return Result.ok(wq);
    }

    /** 错题列表，可按学科/知识点筛选；教师仅能看到本学科 */
    @GetMapping("/list")
    public Result<List<MobileWrongQuestion>> list(@RequestParam(required = false) String subject,
                                                  @RequestParam(required = false) String conceptName) {
        QueryWrapper<MobileWrongQuestion> qw = new QueryWrapper<MobileWrongQuestion>().orderByDesc("id");
        String scope = subjectContext.currentSubject();
        if (scope != null) {
            qw.eq("subject", scope);
        } else if (subject != null && !subject.isEmpty()) {
            qw.eq("subject", subject);
        }
        if (conceptName != null && !conceptName.isEmpty()) qw.eq("concept_name", conceptName);
        return Result.ok(wrongMapper.selectList(qw));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        wrongMapper.deleteById(id);
        return Result.ok();
    }

    /** 班级薄弱点：按知识点聚合错误人数，TOP N */
    @GetMapping("/weakness")
    public Result<Map<String, Object>> weakness(@RequestParam(required = false) String subject,
                                                @RequestParam(defaultValue = "5") int top) {
        QueryWrapper<MobileWrongQuestion> qw = new QueryWrapper<MobileWrongQuestion>()
                .select("concept_name", "SUM(wrong_count) AS cnt")
                .isNotNull("concept_name").ne("concept_name", "")
                .groupBy("concept_name").orderByDesc("cnt").last("LIMIT " + top);
        String scope = subjectContext.currentSubject();
        if (scope != null) {
            qw.eq("subject", scope);
        } else if (subject != null && !subject.isEmpty()) {
            qw.eq("subject", subject);
        }
        List<Map<String, Object>> rows = wrongMapper.selectMaps(qw);
        List<Map<String, Object>> points = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("concept", r.get("concept_name"));
            m.put("count", r.get("cnt"));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("points", points);
        return Result.ok(data);
    }
}
