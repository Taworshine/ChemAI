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

    public ChatController(MobileChatMessageMapper chatMapper, UserMapper userMapper) {
        this.chatMapper = chatMapper;
        this.userMapper = userMapper;
    }

    @PostMapping("/send")
    public Result<MobileChatMessage> send(@RequestBody Map<String, Object> body) {
        MobileChatMessage msg = new MobileChatMessage();
        msg.setMeetingId(Long.valueOf(String.valueOf(body.get("meetingId"))));
        msg.setUserId(StpUtil.getLoginIdAsLong());
        msg.setContent(String.valueOf(body.get("content")));
        msg.setMsgType(String.valueOf(body.getOrDefault("msgType", "TEXT")));
        msg.setReplyTo(body.get("replyTo") == null ? null : Long.valueOf(String.valueOf(body.get("replyTo"))));
        chatMapper.insert(msg);
        return Result.ok(msg);
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long meetingId,
                                                  @RequestParam(required = false) Long afterId) {
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
}
