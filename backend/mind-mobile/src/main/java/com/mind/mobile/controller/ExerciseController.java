package com.mind.mobile.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.QuestionBankMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 习题库（复用 question_bank：自动标注考查知识点） */
@RestController
@RequestMapping("/api/mobile/exercise")
public class ExerciseController {

    private final QuestionBankMapper questionBankMapper;
    private final com.mind.system.service.SubjectContext subjectContext;

    public ExerciseController(QuestionBankMapper questionBankMapper,
                              com.mind.system.service.SubjectContext subjectContext) {
        this.questionBankMapper = questionBankMapper;
        this.subjectContext = subjectContext;
    }

    @GetMapping("/list")
    public Result<List<QuestionBank>> list(@RequestParam(required = false) String subject,
                                           @RequestParam(required = false) String conceptName) {
        QueryWrapper<QuestionBank> qw = new QueryWrapper<QuestionBank>().orderByDesc("id");
        String scope = subjectContext.currentSubject();
        if (scope != null) {
            qw.eq("subject", scope);
        } else if (subject != null && !subject.isEmpty()) {
            qw.eq("subject", subject);
        }
        if (conceptName != null && !conceptName.isEmpty()) qw.eq("concept_name", conceptName);
        return Result.ok(questionBankMapper.selectList(qw));
    }

    @PostMapping("/add")
    public Result<QuestionBank> add(@RequestBody QuestionBank q) {
        q.setId(null);
        // 学科强制继承录入教师的所属学科
        String scope = subjectContext.currentSubject();
        if (scope != null) q.setSubject(scope);
        if (q.getCreateTime() == null) q.setCreateTime(LocalDateTime.now());
        questionBankMapper.insert(q);
        return Result.ok(q);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionBankMapper.deleteById(id);
        return Result.ok();
    }
}
