package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileChatMessage;
import com.mind.mobile.mapper.MobileChatMessageMapper;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会议即时聊天：发送 / 增量轮询拉取
 */
@RestController
@RequestMapping("/api/mobile/chat")
public class ChatController {

    private final MobileChatMessageMapper chatMapper;
    private final UserMapper userMapper;
    private final com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper;

    public ChatController(MobileChatMessageMapper chatMapper,
                          UserMapper userMapper,
                          com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper) {
        this.chatMapper = chatMapper;
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
    }

    /** 发消息：仅参会成员可在会议中发言 */
    @PostMapping("/send")
    public Result<MobileChatMessage> send(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long meetingId = Long.valueOf(String.valueOf(body.get("meetingId")));
        checkMeetingMember(meetingId, userId);
        MobileChatMessage msg = new MobileChatMessage();
        msg.setMeetingId(meetingId);
        msg.setUserId(userId);
        msg.setContent(String.valueOf(body.get("content")));
        msg.setMsgType(String.valueOf(body.getOrDefault("msgType", "TEXT")));
        msg.setReplyTo(body.get("replyTo") == null ? null : Long.valueOf(String.valueOf(body.get("replyTo"))));
        chatMapper.insert(msg);
        return Result.ok(msg);
    }

    /** 消息列表（afterId 增量轮询）：仅参会成员可读 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long meetingId,
                                                  @RequestParam(required = false) Long afterId) {
        checkMeetingMember(meetingId, StpUtil.getLoginIdAsLong());
        QueryWrapper<MobileChatMessage> qw = new QueryWrapper<MobileChatMessage>()
                .eq("meeting_id", meetingId).orderByAsc("id");
        if (afterId != null) {
            qw.gt("id", afterId);
        }
        List<Map<String, Object>> list = chatMapper.selectList(qw).stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            SysUser u = userMapper.selectById(m.getUserId());
            item.put("message", m);
            item.put("realName", u == null ? "" : u.getRealName());
            return item;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    // ---------- 私有方法 ----------

    /** 校验用户是会议成员（所有会议内操作的前提） */
    private void checkMeetingMember(Long meetingId, Long userId) {
        Long count = memberMapper.selectCount(new QueryWrapper<com.mind.mobile.entity.MobileMeetingMember>()
                .eq("meeting_id", meetingId).eq("user_id", userId));
        if (count == null || count == 0) {
            throw new SecurityException("仅会议成员可执行此操作");
        }
    }
}
