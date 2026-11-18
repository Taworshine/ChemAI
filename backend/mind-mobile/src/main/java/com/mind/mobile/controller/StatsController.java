package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.EduDocument;
import com.mind.knowledge.mapper.DocumentMapper;
import com.mind.knowledge.mapper.QuestionBankMapper;
import com.mind.mobile.entity.MobileMeetingMember;
import com.mind.mobile.entity.MobileSummary;
import com.mind.mobile.entity.MobileWrongQuestion;
import com.mind.mobile.mapper.MobileMeetingMemberMapper;
import com.mind.mobile.mapper.MobileSummaryMapper;
import com.mind.mobile.mapper.MobileWrongQuestionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 个人中心数据条：会议 / 教案 / 录题 / 纪要 */
@RestController
@RequestMapping("/api/mobile/user")
public class StatsController {

    private final MobileMeetingMemberMapper memberMapper;
    private final DocumentMapper documentMapper;
    private final QuestionBankMapper questionBankMapper;
    private final MobileSummaryMapper summaryMapper;
    private final MobileWrongQuestionMapper wrongMapper;

    public StatsController(MobileMeetingMemberMapper memberMapper,
                           DocumentMapper documentMapper,
                           QuestionBankMapper questionBankMapper,
                           MobileSummaryMapper summaryMapper,
                           MobileWrongQuestionMapper wrongMapper) {
        this.memberMapper = memberMapper;
        this.documentMapper = documentMapper;
        this.questionBankMapper = questionBankMapper;
        this.summaryMapper = summaryMapper;
        this.wrongMapper = wrongMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long uid = StpUtil.getLoginIdAsLong();
        Map<String, Object> data = new HashMap<>();
        data.put("meetings", memberMapper.selectCount(
                new QueryWrapper<MobileMeetingMember>().eq("user_id", uid)));
        data.put("plans", documentMapper.selectCount(
                new QueryWrapper<EduDocument>().eq("upload_user_id", uid)));
        data.put("questions", questionBankMapper.selectCount(null)
                + wrongMapper.selectCount(null));
        data.put("minutes", summaryMapper.selectCount(null));
        return Result.ok(data);
    }
}
