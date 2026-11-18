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

    public TaskController(MobileTaskMapper taskMapper, UserMapper userMapper) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
    }

    /** 我的任务（默认）；主持人传 meetingId 看会议全部任务 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long meetingId) {
        QueryWrapper<MobileTask> qw = new QueryWrapper<MobileTask>().orderByDesc("id");
        if (meetingId != null) {
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

    /** 创建任务（通常由主持人在会议中手动添加） */
    @PostMapping("/create")
    public Result<MobileTask> create(@RequestBody Map<String, Object> body) {
        MobileTask task = new MobileTask();
        task.setMeetingId(body.get("meetingId") == null ? null
                : Long.valueOf(String.valueOf(body.get("meetingId"))));
        task.setTitle(String.valueOf(body.get("title")));
        task.setAssigneeId(Long.valueOf(String.valueOf(body.get("assigneeId"))));
        String deadline = body.get("deadline") == null ? null : String.valueOf(body.get("deadline"));
        if (deadline != null && !deadline.isEmpty() && !"null".equals(deadline)) {
            task.setDeadline(java.time.LocalDateTime.parse(deadline.replace(' ', 'T').substring(0, 19)));
        }
        task.setStatus(0);
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
}
