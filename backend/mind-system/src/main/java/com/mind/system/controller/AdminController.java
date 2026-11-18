package com.mind.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mind.common.Result;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import com.mind.system.service.SubjectContext;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员后台：教师学科分配。
 * 仅 ADMIN 角色可访问；教师本人无法修改自己的学科。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final SubjectContext subjectContext;

    public AdminController(UserMapper userMapper, SubjectContext subjectContext) {
        this.userMapper = userMapper;
        this.subjectContext = subjectContext;
    }

    /** 学科字典（分配下拉选项） */
    @GetMapping("/subjects")
    public Result<List<String>> subjects() {
        if (!subjectContext.isAdmin()) return Result.error(403, "仅管理员可访问");
        return Result.ok(Arrays.asList(SubjectContext.SUBJECTS));
    }

    /** 教师列表（含当前学科） */
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> teachers() {
        if (!subjectContext.isAdmin()) return Result.error(403, "仅管理员可访问");
        List<SysUser> users = userMapper.selectList(
                new QueryWrapper<SysUser>().eq("role", "TEACHER").orderByAsc("id"));
        return Result.ok(users.stream().map(u -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("className", u.getClassName());
            m.put("subject", u.getSubject());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 给教师分配学科（传空字符串表示取消分配） */
    @PutMapping("/teachers/{id}/subject")
    public Result<?> assignSubject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!subjectContext.isAdmin()) return Result.error(403, "仅管理员可访问");
        SysUser teacher = userMapper.selectById(id);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())) {
            return Result.error(404, "教师不存在");
        }
        String subject = body.get("subject");
        if (subject != null && !subject.isEmpty() &&
                Arrays.stream(SubjectContext.SUBJECTS).noneMatch(s -> s.equals(subject))) {
            return Result.error("无效的学科：" + subject);
        }
        SysUser upd = new SysUser();
        upd.setSubject(subject == null || subject.isEmpty() ? null : subject);
        userMapper.update(upd, new UpdateWrapper<SysUser>().eq("id", id));
        return Result.ok();
    }
}
