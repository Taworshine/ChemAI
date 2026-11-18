package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileTask;
import com.mind.mobile.mapper.MobileTaskMapper;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会议任务：列表 / 主持人创建 / 完成回写
 */
@RestController
@RequestMapping("/api/mobile/task")
public class TaskController {

    private final MobileTaskMapper taskMapper;
    private final UserMapper userMapper;
    private final com.mind.mobile.mapper.MobileMeetingMapper meetingMapper;
    private final com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper;

    public TaskController(MobileTaskMapper taskMapper,
                          UserMapper userMapper,
                          com.mind.mobile.mapper.MobileMeetingMapper meetingMapper,
                          com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.meetingMapper = meetingMapper;
        this.memberMapper = memberMapper;
    }

    /** 我的任务（默认）；传 meetingId 看会议全部任务（仅会议成员可见） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long meetingId) {
        QueryWrapper<MobileTask> qw = new QueryWrapper<MobileTask>().orderByDesc("id");
        if (meetingId != null) {
            checkMeetingMember(meetingId, StpUtil.getLoginIdAsLong());
            qw.eq("meeting_id", meetingId);
        } else {
            qw.eq("assignee_id", StpUtil.getLoginIdAsLong());
        }
        List<Map<String, Object>> list = taskMapper.selectList(qw).stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            SysUser u = userMapper.selectById(t.getAssigneeId());
            item.put("task", t);
            item.put("assigneeName", u == null ? "" : u.getRealName());
            return item;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    /** 创建任务：仅会议主持人可指派，负责人必须是参会成员 */
    @PostMapping("/create")
    public Result<MobileTask> create(@RequestBody Map<String, Object> body) {
        Long meetingId = body.get("meetingId") == null ? null
                : Long.valueOf(String.valueOf(body.get("meetingId")));
        Long assigneeId = Long.valueOf(String.valueOf(body.get("assigneeId")));
        if (meetingId == null) {
            return Result.error("任务必须归属会议");
        }
        com.mind.mobile.entity.MobileMeeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        if (!meeting.getHostId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error(403, "仅主持人可指派任务");
        }
        Long memberCount = memberMapper.selectCount(new QueryWrapper<com.mind.mobile.entity.MobileMeetingMember>()
                .eq("meeting_id", meetingId).eq("user_id", assigneeId));
        if (memberCount == null || memberCount == 0) {
            return Result.error("负责人必须是参会成员");
        }
        MobileTask task = new MobileTask();
        task.setMeetingId(meetingId);
        task.setTitle(String.valueOf(body.get("title")));
        task.setAssigneeId(Long.valueOf(String.valueOf(body.get("assigneeId"))));
        String deadline = body.get("deadline") == null ? null : String.valueOf(body.get("deadline"));
        if (deadline != null && !deadline.isEmpty() && !"null".equals(deadline)) {
            task.setDeadline(java.time.LocalDateTime.parse(deadline.replace(' ', 'T').substring(0, 19)));
        }
        task.setStatus(0);
        task.setAssigneeId(assigneeId);
        taskMapper.insert(task);
        return Result.ok(task);
    }

    /** 完成任务（负责人本人） */
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable("id") Long id) {
        MobileTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (!task.getAssigneeId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error("仅负责人可完成任务");
        }
        taskMapper.update(null, new UpdateWrapper<MobileTask>().eq("id", id).set("status", 2));
        return Result.ok();
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
