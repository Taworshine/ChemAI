package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.QuestionBankMapper;
import com.mind.mobile.entity.MobilePlanLink;
import com.mind.mobile.entity.MobileWrongQuestion;
import com.mind.mobile.mapper.MobilePlanLinkMapper;
import com.mind.mobile.mapper.MobileWrongQuestionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 教案-题目关联 */
@RestController
@RequestMapping("/api/mobile/plan-link")
public class PlanLinkController {

    private final MobilePlanLinkMapper linkMapper;
    private final MobileWrongQuestionMapper wrongMapper;
    private final QuestionBankMapper questionBankMapper;

    public PlanLinkController(MobilePlanLinkMapper linkMapper,
                              MobileWrongQuestionMapper wrongMapper,
                              QuestionBankMapper questionBankMapper) {
        this.linkMapper = linkMapper;
        this.wrongMapper = wrongMapper;
        this.questionBankMapper = questionBankMapper;
    }

    /** 关联题目到教案（幂等，唯一键去重） */
    @PostMapping("/add")
    public Result<?> add(@RequestBody MobilePlanLink link) {
        Long exists = linkMapper.selectCount(new QueryWrapper<MobilePlanLink>()
                .eq("plan_id", link.getPlanId())
                .eq("target_type", link.getTargetType())
                .eq("target_id", link.getTargetId()));
        if (exists == 0) {
            link.setId(null);
            link.setCreateBy(StpUtil.getLoginIdAsLong());
            link.setCreateTime(LocalDateTime.now());
            linkMapper.insert(link);
        }
        return Result.ok();
    }

    /** 教案已关联的题目列表 */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long planId) {
        List<MobilePlanLink> links = linkMapper.selectList(
                new QueryWrapper<MobilePlanLink>().eq("plan_id", planId).orderByDesc("id"));
        List<Map<String, Object>> wrongs = new ArrayList<>();
        List<Map<String, Object>> exercises = new ArrayList<>();
        for (MobilePlanLink l : links) {
            Map<String, Object> item = new HashMap<>();
            item.put("linkId", l.getId());
            if ("WRONG".equals(l.getTargetType())) {
                MobileWrongQuestion w = wrongMapper.selectById(l.getTargetId());
                if (w != null) { item.put("item", w); wrongs.add(item); }
            } else {
                QuestionBank q = questionBankMapper.selectById(l.getTargetId());
                if (q != null) { item.put("item", q); exercises.add(item); }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("wrongs", wrongs);
        data.put("exercises", exercises);
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        linkMapper.deleteById(id);
        return Result.ok();
    }
}
