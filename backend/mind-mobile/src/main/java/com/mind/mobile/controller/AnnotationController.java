package com.mind.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mind.common.Result;
import com.mind.mobile.entity.MobileAnnotation;
import com.mind.mobile.mapper.MobileAnnotationMapper;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教案批注：手绘（相对坐标）/ 文字 / 60秒语音，回复、解决、轮询同步
 */
@RestController
@RequestMapping("/api/mobile/annotation")
public class AnnotationController {

    private final MobileAnnotationMapper annotationMapper;
    private final UserMapper userMapper;
    private final com.mind.mobile.mapper.MobileMeetingMapper meetingMapper;
    private final com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper;
    private final String uploadDir;

    public AnnotationController(MobileAnnotationMapper annotationMapper,
                                UserMapper userMapper,
                                com.mind.mobile.mapper.MobileMeetingMapper meetingMapper,
                                com.mind.mobile.mapper.MobileMeetingMemberMapper memberMapper,
                                @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.annotationMapper = annotationMapper;
        this.userMapper = userMapper;
        this.meetingMapper = meetingMapper;
        this.memberMapper = memberMapper;
        this.uploadDir = uploadDir;
    }

    /** 新增批注（DRAW 带 drawData JSON；VOICE 可走 /voice 接口先传音频拿路径）。仅参会成员可批注 */
    @PostMapping("/add")
    public Result<MobileAnnotation> add(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long meetingId = Long.valueOf(String.valueOf(body.get("meetingId")));
        checkMeetingMember(meetingId, userId);
        MobileAnnotation a = new MobileAnnotation();
        a.setMeetingId(meetingId);
        a.setDocId(body.get("docId") == null ? null : Long.valueOf(String.valueOf(body.get("docId"))));
        a.setUserId(userId);
        a.setType(String.valueOf(body.getOrDefault("type", "TEXT")));
        a.setPage(body.get("page") == null ? 1 : Integer.valueOf(String.valueOf(body.get("page"))));
        a.setColor(String.valueOf(body.getOrDefault("color", "#ff4d4f")));
        a.setStrokeWidth(body.get("strokeWidth") == null ? 4
                : Integer.valueOf(String.valueOf(body.get("strokeWidth"))));
        a.setDrawData(body.get("drawData") == null ? null : String.valueOf(body.get("drawData")));
        a.setContent(body.get("content") == null ? null : String.valueOf(body.get("content")));
        a.setVoicePath(body.get("voicePath") == null ? null : String.valueOf(body.get("voicePath")));
        a.setVoiceSeconds(body.get("voiceSeconds") == null ? 0
                : Integer.valueOf(String.valueOf(body.get("voiceSeconds"))));
        a.setReplyTo(body.get("replyTo") == null ? null : Long.valueOf(String.valueOf(body.get("replyTo"))));
        a.setIsResolved(0);
        annotationMapper.insert(a);
        return Result.ok(a);
    }

    /** 批注列表（afterId 用于轮询增量拉取） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long meetingId,
                                                  @RequestParam(required = false) Long docId,
                                                  @RequestParam(required = false) Long afterId) {
        QueryWrapper<MobileAnnotation> qw = new QueryWrapper<MobileAnnotation>()
                .eq("meeting_id", meetingId).orderByAsc("id");
        if (docId != null) {
            qw.eq("doc_id", docId);
        }
        if (afterId != null) {
            qw.gt("id", afterId);
        }
        List<Map<String, Object>> list = annotationMapper.selectList(qw).stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            SysUser u = userMapper.selectById(a.getUserId());
            item.put("annotation", a);
            item.put("realName", u == null ? "" : u.getRealName());
            return item;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    /** 语音批注音频上传（≤60秒），返回可访问路径 */
    @PostMapping("/voice")
    public Result<Map<String, Object>> voice(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("语音文件过大（限 10MB）");
        }
        String original = file.getOriginalFilename() == null ? "voice" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".m4a";
        Path dir = Paths.get(uploadDir, "voice").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        Map<String, Object> data = new HashMap<>();
        data.put("voicePath", target.toString());
        return Result.ok(data);
    }

    /** 回复批注（本质是一条 replyTo 指向原批注的新批注）。仅参会成员可回复 */
    @PostMapping("/{id}/reply")
    public Result<MobileAnnotation> reply(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        MobileAnnotation origin = annotationMapper.selectById(id);
        if (origin == null) {
            return Result.error("批注不存在");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        checkMeetingMember(origin.getMeetingId(), userId);
        MobileAnnotation a = new MobileAnnotation();
        a.setMeetingId(origin.getMeetingId());
        a.setDocId(origin.getDocId());
        a.setUserId(userId);
        a.setType("TEXT");
        a.setPage(origin.getPage());
        a.setColor("#2b5aed");
        a.setStrokeWidth(4);
        a.setContent(String.valueOf(body.get("content")));
        a.setReplyTo(id);
        a.setIsResolved(0);
        annotationMapper.insert(a);
        return Result.ok(a);
    }

    /** 标记解决 / 取消解决：仅批注作者本人或会议主持人可操作 */
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Integer> body) {
        MobileAnnotation a = annotationMapper.selectById(id);
        if (a == null) {
            return Result.error("批注不存在");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        com.mind.mobile.entity.MobileMeeting meeting = meetingMapper.selectById(a.getMeetingId());
        boolean isHost = meeting != null && meeting.getHostId().equals(userId);
        if (!a.getUserId().equals(userId) && !isHost) {
            return Result.error(403, "仅批注作者或主持人可标记解决");
        }
        int resolved = body == null || body.get("resolved") == null ? 1 : body.get("resolved");
        annotationMapper.update(null, new UpdateWrapper<MobileAnnotation>()
                .eq("id", id).set("is_resolved", resolved));
        return Result.ok();
    }

    /** 删除自己创建的批注 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        MobileAnnotation a = annotationMapper.selectById(id);
        if (a == null) {
            return Result.ok();
        }
        if (!a.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.error("只能删除自己的批注");
        }
        annotationMapper.deleteById(id);
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
