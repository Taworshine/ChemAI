package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mind.common.Result;
import com.mind.mobile.entity.*;
import com.mind.mobile.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会议纪要：规则引擎自动汇总（背景/讨论/决议/任务），主持人确认后分发
 */
@RestController
@RequestMapping("/api/mobile/summary")
public class SummaryController {

    private final MobileSummaryMapper summaryMapper;
    private final MobileMeetingMapper meetingMapper;
    private final MobileChatMessageMapper chatMapper;
    private final MobileAnnotationMapper annotationMapper;
    private final MobileVoteMapper voteMapper;
    private final MobileTaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SummaryController(MobileSummaryMapper summaryMapper,
                             MobileMeetingMapper meetingMapper,
                             MobileChatMessageMapper chatMapper,
                             MobileAnnotationMapper annotationMapper,
                             MobileVoteMapper voteMapper,
                             MobileTaskMapper taskMapper) {
        this.summaryMapper = summaryMapper;
        this.meetingMapper = meetingMapper;
        this.chatMapper = chatMapper;
        this.annotationMapper = annotationMapper;
        this.voteMapper = voteMapper;
        this.taskMapper = taskMapper;
    }

    /** 生成（或重新生成）纪要：从聊天记录、批注、投票、任务中规则提取 */
    @PostMapping("/generate")
    public Result<MobileSummary> generate(@RequestBody Map<String, Long> body) throws Exception {
        Long meetingId = body.get("meetingId");
        MobileMeeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error(400, "会议不存在");
        }
        if (!meeting.getHostId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error(403, "仅主持人可生成纪要");
        }

        ObjectNode content = objectMapper.createObjectNode();
        content.put("background", meeting.getSubject() + "教研组会议：" + meeting.getTitle()
                + "（议程：" + meeting.getAgenda() + "）");

        ArrayNode discussion = content.putArray("discussion");
        List<MobileAnnotation> annotations = annotationMapper.selectList(
                new QueryWrapper<MobileAnnotation>().eq("meeting_id", meetingId));
        for (MobileAnnotation a : annotations) {
            if ("TEXT".equals(a.getType()) && a.getContent() != null && a.getReplyTo() == null) {
                discussion.add("批注：" + a.getContent());
            }
        }
        List<MobileChatMessage> chats = chatMapper.selectList(
                new QueryWrapper<MobileChatMessage>().eq("meeting_id", meetingId));
        for (MobileChatMessage m : chats) {
            if (m.getContent() != null && m.getContent().length() >= 10) {
                discussion.add("讨论：" + m.getContent());
            }
        }

        ArrayNode decisions = content.putArray("decisions");
        List<MobileVote> votes = voteMapper.selectList(
                new QueryWrapper<MobileVote>().eq("meeting_id", meetingId));
        for (MobileVote v : votes) {
            if (v.getStatus() == 1) {
                decisions.add("投票《 " + v.getTitle() + " 》已结束");
            }
        }

        ArrayNode tasks = content.putArray("tasks");
        List<MobileTask> taskList = taskMapper.selectList(
                new QueryWrapper<MobileTask>().eq("meeting_id", meetingId));
        for (MobileTask t : taskList) {
            tasks.add(t.getTitle() + "（负责人ID：" + t.getAssigneeId()
                    + (t.getDeadline() != null ? "，截止：" + t.getDeadline() : "") + "）");
        }

        MobileSummary summary = summaryMapper.selectOne(
                new QueryWrapper<MobileSummary>().eq("meeting_id", meetingId));
        if (summary == null) {
            summary = new MobileSummary();
            summary.setMeetingId(meetingId);
            summary.setAutoGenerate(1);
            summary.setIsConfirmed(0);
        }
        summary.setContent(objectMapper.writeValueAsString(content));
        if (summary.getId() == null) {
            summaryMapper.insert(summary);
        } else {
            summary.setIsConfirmed(0);
            summaryMapper.updateById(summary);
        }
        return Result.ok(summary);
    }

    /** 纪要历史（我参加的会议的纪要） */
    @GetMapping("/list")
    public Result<List<MobileSummary>> list() {
        return Result.ok(summaryMapper.selectList(
                new QueryWrapper<MobileSummary>().orderByDesc("id")));
    }

    /** 查看纪要 */
    @GetMapping("/{meetingId}")
    public Result<MobileSummary> get(@PathVariable("meetingId") Long meetingId) {
        return Result.ok(summaryMapper.selectOne(
                new QueryWrapper<MobileSummary>().eq("meeting_id", meetingId)));
    }

    /** 主持人编辑并确认纪要（content 为完整 JSON 字符串） */
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestBody Map<String, Object> body) {
        Long meetingId = Long.valueOf(String.valueOf(body.get("meetingId")));
        MobileSummary summary = summaryMapper.selectOne(
                new QueryWrapper<MobileSummary>().eq("meeting_id", meetingId));
        if (summary == null) {
            return Result.error(400, "纪要不存在，请先生成");
        }
        MobileMeeting meeting = meetingMapper.selectById(meetingId);
        if (!meeting.getHostId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error(403, "仅主持人可确认纪要");
        }
        if (body.get("content") != null) {
            summary.setContent(String.valueOf(body.get("content")));
        }
        summary.setAutoGenerate(0);
        summary.setIsConfirmed(1);
        summaryMapper.updateById(summary);
        return Result.ok();
    }
}
