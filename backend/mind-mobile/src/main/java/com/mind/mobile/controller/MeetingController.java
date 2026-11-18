package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileMeeting;
import com.mind.mobile.entity.MobileMeetingMember;
import com.mind.mobile.mapper.MobileMeetingMapper;
import com.mind.mobile.mapper.MobileMeetingMemberMapper;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 会议管理：创建（生成6位会议号）/ 输码加入 / 列表 / 状态 / 主持人控制
 */
@RestController
@RequestMapping("/api/mobile/meeting")
public class MeetingController {

    private final MobileMeetingMapper meetingMapper;
    private final MobileMeetingMemberMapper memberMapper;
    private final UserMapper userMapper;

    public MeetingController(MobileMeetingMapper meetingMapper,
                             MobileMeetingMemberMapper memberMapper,
                             UserMapper userMapper) {
        this.meetingMapper = meetingMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    /** 创建会议：生成不重复的 6 位数字会议号。仅教师可发起教研会议 */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser creator = userMapper.selectById(userId);
        if (creator == null || !"TEACHER".equals(creator.getRole())) {
            return Result.error(403, "仅教师可发起教研会议");
        }
        MobileMeeting meeting = new MobileMeeting();
        meeting.setTitle(String.valueOf(body.get("title")));
        Object subject = body.get("subject");
        // 未传学科时自动继承主持人所属学科
        if (subject == null || String.valueOf(subject).isEmpty() || "null".equals(String.valueOf(subject))) {
            meeting.setSubject(creator.getSubject());
        } else {
            meeting.setSubject(String.valueOf(subject));
        }
        meeting.setAgenda(String.valueOf(body.getOrDefault("agenda", "LESSON_REVIEW")));
        meeting.setHostId(userId);
        meeting.setStatus(0);
        meeting.setLocked(0);
        meeting.setMeetingNo(genMeetingNo());
        meetingMapper.insert(meeting);

        addMember(meeting.getId(), userId, "HOST");
        @SuppressWarnings("unchecked")
        List<Integer> memberIds = body.get("memberIds") == null
                ? new ArrayList<>() : (List<Integer>) body.get("memberIds");
        for (Integer mid : memberIds) {
            // 只能邀请教师参会（教研会是教师活动），且不能重复添加主持人
            if (mid.equals(userId.intValue())) continue;
            SysUser invitee = userMapper.selectById(mid.longValue());
            if (invitee != null && "TEACHER".equals(invitee.getRole())) {
                addMember(meeting.getId(), mid.longValue(), "MEMBER");
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("meeting", meeting);
        data.put("members", memberList(meeting.getId()));
        return Result.ok(data);
    }

    /** 输 6 位会议号加入。仅教师可参加教研会议 */
    @PostMapping("/join")
    public Result<Map<String, Object>> join(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser joiner = userMapper.selectById(userId);
        if (joiner == null || !"TEACHER".equals(joiner.getRole())) {
            return Result.error(403, "仅教师可参加教研会议");
        }
        String meetingNo = body.get("meetingNo");
        MobileMeeting meeting = meetingMapper.selectOne(
                new QueryWrapper<MobileMeeting>().eq("meeting_no", meetingNo));
        if (meeting == null) {
            return Result.error("会议号不存在");
        }
        if (meeting.getLocked() != null && meeting.getLocked() == 1) {
            return Result.error("会议已锁定，无法加入");
        }
        if (meeting.getStatus() == 2 || meeting.getStatus() == 3) {
            return Result.error("会议已结束");
        }
        MobileMeetingMember exist = memberMapper.selectOne(new QueryWrapper<MobileMeetingMember>()
                .eq("meeting_id", meeting.getId()).eq("user_id", userId));
        if (exist == null) {
            addMember(meeting.getId(), userId, "MEMBER");
        } else {
            exist.setOnline(1);
            memberMapper.updateById(exist);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("meeting", meeting);
        data.put("members", memberList(meeting.getId()));
        return Result.ok(data);
    }

    /** 我的会议列表：tab = host(我主持的) / member(我参与的) / history(历史) */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "host") String tab) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<MobileMeetingMember> myMemberships = memberMapper.selectList(
                new QueryWrapper<MobileMeetingMember>().eq("user_id", userId));
        Set<Long> meetingIds = myMemberships.stream().map(MobileMeetingMember::getMeetingId)
                .collect(Collectors.toSet());
        if (meetingIds.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        QueryWrapper<MobileMeeting> qw = new QueryWrapper<MobileMeeting>().in("id", meetingIds)
                .orderByDesc("id");
        if ("host".equals(tab)) {
            qw.eq("host_id", userId).in("status", 0, 1);
        } else if ("member".equals(tab)) {
            qw.ne("host_id", userId).in("status", 0, 1);
        } else {
            qw.eq("status", 2);
        }
        List<Map<String, Object>> list = meetingMapper.selectList(qw).stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("meeting", m);
            item.put("members", memberList(m.getId()));
            return item;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    /** 会议详情与状态（轮询入口） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> status(@PathVariable("id") Long id) {
        MobileMeeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("meeting", meeting);
        data.put("members", memberList(id));
        data.put("isHost", meeting.getHostId().equals(StpUtil.getLoginIdAsLong()));
        return Result.ok(data);
    }

    /** 主持人：开始 / 结束 / 锁定 / 切换当前教案 */
    @PostMapping("/{id}/control")
    public Result<Void> control(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        MobileMeeting meeting = checkHost(id);
        String action = String.valueOf(body.get("action"));
        switch (action) {
            case "start" -> {
                meeting.setStatus(1);
                meeting.setStartTime(LocalDateTime.now());
            }
            case "close" -> {
                meeting.setStatus(2);
                meeting.setEndTime(LocalDateTime.now());
                memberMapper.update(null, new UpdateWrapper<MobileMeetingMember>()
                        .eq("meeting_id", id).set("online", 0));
            }
            case "lock" -> meeting.setLocked(1);
            case "unlock" -> meeting.setLocked(0);
            case "switchDoc" -> meeting.setCurrentDocId(
                    body.get("docId") == null ? null : Long.valueOf(String.valueOf(body.get("docId"))));
            default -> { return Result.error("不支持的操作：" + action); }
        }
        meetingMapper.updateById(meeting);
        return Result.ok();
    }

    /** 主持人：移出成员（不能移出主持人自己，避免会议无主） */
    @PostMapping("/{id}/kick")
    public Result<Void> kick(@PathVariable("id") Long id, @RequestBody Map<String, Long> body) {
        MobileMeeting meeting = checkHost(id);
        Long userId = body.get("userId");
        if (userId == null) {
            return Result.error("缺少 userId");
        }
        if (meeting.getHostId().equals(userId)) {
            return Result.error("不能移出主持人自己");
        }
        memberMapper.delete(new QueryWrapper<MobileMeetingMember>()
                .eq("meeting_id", id).eq("user_id", userId));
        return Result.ok();
    }

    /** 供创建会议时选择参会教师（复用 sys_user，教师角色） */
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> teachers() {
        List<Map<String, Object>> list = userMapper.selectList(
                        new QueryWrapper<SysUser>().eq("role", "TEACHER")).stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", u.getId());
                    m.put("realName", u.getRealName());
                    m.put("username", u.getUsername());
                    return m;
                }).collect(Collectors.toList());
        return Result.ok(list);
    }

    // ---------- 私有方法 ----------

    private void addMember(Long meetingId, Long userId, String role) {
        MobileMeetingMember m = new MobileMeetingMember();
        m.setMeetingId(meetingId);
        m.setUserId(userId);
        m.setRole(role);
        m.setOnline(1);
        memberMapper.insert(m);
    }

    private List<Map<String, Object>> memberList(Long meetingId) {
        return memberMapper.selectList(new QueryWrapper<MobileMeetingMember>()
                        .eq("meeting_id", meetingId)).stream().map(m -> {
                    Map<String, Object> item = new HashMap<>();
                    SysUser u = userMapper.selectById(m.getUserId());
                    item.put("userId", m.getUserId());
                    item.put("role", m.getRole());
                    item.put("online", m.getOnline());
                    item.put("realName", u == null ? "已注销用户" : u.getRealName());
                    return item;
                }).collect(Collectors.toList());
    }

    private MobileMeeting checkHost(Long id) {
        MobileMeeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new IllegalArgumentException("会议不存在");
        }
        if (!meeting.getHostId().equals(StpUtil.getLoginIdAsLong())) {
            throw new SecurityException("仅主持人可操作");
        }
        return meeting;
    }

    private String genMeetingNo() {
        Random random = new Random();
        String no;
        do {
            no = String.format("%06d", random.nextInt(1000000));
        } while (meetingMapper.selectCount(
                new QueryWrapper<MobileMeeting>().eq("meeting_no", no)) > 0);
        return no;
    }
}
