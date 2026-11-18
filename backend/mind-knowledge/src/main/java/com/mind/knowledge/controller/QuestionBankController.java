package com.mind.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.QuestionBankMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理：多学科题目的增删改查
 */
@RestController
@RequestMapping("/api/question")
public class QuestionBankController {

    private final QuestionBankMapper questionBankMapper;
    private final com.mind.system.service.SubjectContext subjectContext;

    public QuestionBankController(QuestionBankMapper questionBankMapper,
                                  com.mind.system.service.SubjectContext subjectContext) {
        this.questionBankMapper = questionBankMapper;
        this.subjectContext = subjectContext;
    }

    /** 分页查询（教师 / 学生抽题共用，学生端不返回答案由 practice 接口控制） */
    @GetMapping("/list")
    public Result<List<QuestionBank>> list(@RequestParam(required = false) String subject,
                                           @RequestParam(required = false) String keyword) {
        QueryWrapper<QuestionBank> qw = new QueryWrapper<>();
        // 教师仅见本学科题目；管理员/学生不做限制
        String scope = subjectContext.currentSubject();
        if (scope != null) {
            qw.eq("subject", scope);
        } else if (subject != null && !subject.isEmpty()) qw.eq("subject", subject);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("content", keyword).or().like("concept_name", keyword));
        }
        qw.orderByDesc("id");
        return Result.ok(questionBankMapper.selectList(qw));
    }

    @PostMapping
    public Result<QuestionBank> create(@RequestBody QuestionBank question) {
        question.setId(null);
        // 学科强制继承录入教师的所属学科
        String scope = subjectContext.currentSubject();
        if (scope != null) question.setSubject(scope);
        questionBankMapper.insert(question);
        return Result.ok(question);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody QuestionBank question) {
        question.setId(id);
        questionBankMapper.updateById(question);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        questionBankMapper.deleteById(id);
        return Result.ok();
    }
}
