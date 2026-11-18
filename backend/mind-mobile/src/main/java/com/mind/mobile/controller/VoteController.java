package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileVote;
import com.mind.mobile.entity.MobileVoteRecord;
import com.mind.mobile.mapper.MobileVoteMapper;
import com.mind.mobile.mapper.MobileVoteRecordMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时投票：单选/多选/评分，匿名/实名，倒计时自动结束，实时结果
 */
@RestController
@RequestMapping("/api/mobile/vote")
public class VoteController {

    private final MobileVoteMapper voteMapper;
    private final MobileVoteRecordMapper recordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VoteController(MobileVoteMapper voteMapper, MobileVoteRecordMapper recordMapper) {
        this.voteMapper = voteMapper;
        this.recordMapper = recordMapper;
    }

    /** 创建投票：options = [{text:"..."}]，后台自动补 id */
    @PostMapping("/create")
    public Result<MobileVote> create(@RequestBody Map<String, Object> body) throws Exception {
        MobileVote vote = new MobileVote();
        vote.setMeetingId(Long.valueOf(String.valueOf(body.get("meetingId"))));
        vote.setCreatorId(StpUtil.getLoginIdAsLong());
        vote.setTitle(String.valueOf(body.get("title")));
        vote.setVoteType(String.valueOf(body.getOrDefault("voteType", "SINGLE")));
        vote.setAnonymous(body.get("anonymous") == null ? 1
                : Integer.valueOf(String.valueOf(body.get("anonymous"))));
        String deadline = body.get("deadline") == null ? null : String.valueOf(body.get("deadline"));
        if (deadline != null && !deadline.isEmpty() && !"null".equals(deadline)) {
            vote.setDeadline(LocalDateTime.parse(deadline.replace(' ', 'T').substring(0, 19)));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) body.get("options");
        List<Map<String, Object>> normalized = new ArrayList<>();
        String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h"};
        for (int i = 0; i < options.size() && i < letters.length; i++) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("id", letters[i]);
            opt.put("text", String.valueOf(options.get(i).get("text")));
            normalized.add(opt);
        }
        vote.setOptions(objectMapper.writeValueAsString(normalized));
        vote.setStatus(0);
        voteMapper.insert(vote);
        return Result.ok(vote);
    }

    /** 提交投票：choices = [{optionId, score}] */
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long voteId = Long.valueOf(String.valueOf(body.get("voteId")));
        MobileVote vote = checkOpen(voteId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        for (Map<String, Object> c : choices) {
            MobileVoteRecord record = new MobileVoteRecord();
            record.setVoteId(voteId);
            record.setMeetingId(vote.getMeetingId());
            record.setUserId(userId);
            record.setOptionId(String.valueOf(c.get("optionId")));
            record.setScore(c.get("score") == null ? null
                    : Integer.valueOf(String.valueOf(c.get("score"))));
            try {
                recordMapper.insert(record);
            } catch (Exception ignored) {
                // 唯一键冲突：重复投同一选项，忽略
            }
        }
        return Result.ok();
    }

    /** 投票结果（匿名时只返回聚合；附带当前用户是否已投） */
    @GetMapping("/{id}/result")
    public Result<Map<String, Object>> result(@PathVariable("id") Long id) throws Exception {
        MobileVote vote = voteMapper.selectById(id);
        if (vote == null) {
            return Result.error("投票不存在");
        }
        autoCloseIfExpired(vote);
        List<MobileVoteRecord> records = recordMapper.selectList(
                new QueryWrapper<MobileVoteRecord>().eq("vote_id", id));

        Map<String, Object> data = new HashMap<>();
        data.put("vote", vote);
        data.put("totalVoters", records.stream().map(MobileVoteRecord::getUserId).distinct().count());

        List<Map<String, Object>> optionResults = new ArrayList<>();
        JsonNode options = objectMapper.readTree(vote.getOptions());
        for (JsonNode opt : options) {
            String optionId = opt.get("id").asText();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("optionId", optionId);
            item.put("text", opt.get("text").asText());
            List<MobileVoteRecord> matched = records.stream()
                    .filter(r -> optionId.equals(r.getOptionId())).collect(Collectors.toList());
            item.put("count", matched.size());
            item.put("avgScore", matched.stream().filter(r -> r.getScore() != null)
                    .mapToInt(MobileVoteRecord::getScore).average().orElse(0));
            if (vote.getAnonymous() == 0) {
                item.put("voters", matched.stream().map(MobileVoteRecord::getUserId)
                        .collect(Collectors.toList()));
            }
            optionResults.add(item);
        }
        data.put("options", optionResults);
        data.put("myVoted", records.stream().anyMatch(r -> r.getUserId().equals(StpUtil.getLoginIdAsLong())));
        return Result.ok(data);
    }

    /** 列出会议全部投票 */
    @GetMapping("/list")
    public Result<List<MobileVote>> list(@RequestParam Long meetingId) {
        List<MobileVote> votes = voteMapper.selectList(new QueryWrapper<MobileVote>()
                .eq("meeting_id", meetingId).orderByDesc("id"));
        votes.forEach(this::autoCloseIfExpired);
        return Result.ok(votes);
    }

    /** 提前结束投票（创建者） */
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable("id") Long id) {
        MobileVote vote = voteMapper.selectById(id);
        if (vote == null) {
            return Result.error("投票不存在");
        }
        if (!vote.getCreatorId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error("仅创建者可结束投票");
        }
        voteMapper.update(null, new UpdateWrapper<MobileVote>().eq("id", id).set("status", 1));
        return Result.ok();
    }

    // ---------- 私有方法 ----------

    private MobileVote checkOpen(Long voteId) {
        MobileVote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw new IllegalArgumentException("投票不存在");
        }
        autoCloseIfExpired(vote);
        if (vote.getStatus() == 1) {
            throw new IllegalStateException("投票已结束");
        }
        return vote;
    }

    private void autoCloseIfExpired(MobileVote vote) {
        if (vote.getStatus() == 0 && vote.getDeadline() != null
                && vote.getDeadline().isBefore(LocalDateTime.now())) {
            vote.setStatus(1);
            voteMapper.update(null, new UpdateWrapper<MobileVote>()
                    .eq("id", vote.getId()).set("status", 1));
        }
    }
}
