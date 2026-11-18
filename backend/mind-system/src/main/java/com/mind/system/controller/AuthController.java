package com.mind.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mind.common.Result;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：登录 / 当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** SHA-256 摘要（演示项目，生产应使用 BCrypt + 盐） */
    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null || !user.getPassword().equals(sha256(password))) {
            return Result.error(401, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("role", user.getRole());
        data.put("realName", user.getRealName());
        data.put("userId", user.getId());
        data.put("subject", user.getSubject());
        return Result.ok(data);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("className", user.getClassName());
        data.put("subject", user.getSubject());
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 自助注册：学生或教师自己创建账号（后台管理员由种子数据预置，不走这里）。
     * 用户名全局唯一；密码 SHA-256 存储；教师可填所属学科。
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String role = body.get("role") == null ? "STUDENT" : body.get("role").toUpperCase();
        String className = body.get("className");
        String subject = body.get("subject");

        if (username == null || username.trim().length() < 3 || username.trim().length() > 50) {
            return Result.error("用户名需为 3~50 个字符");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码至少 6 位");
        }
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            return Result.error("注册身份只支持学生或教师");
        }
        Long exists = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username.trim()));
        if (exists != null && exists > 0) {
            return Result.error("该用户名已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(sha256(password));
        user.setRealName(realName == null || realName.isEmpty() ? username.trim() : realName.trim());
        user.setRole(role);
        user.setClassName(className == null || className.isEmpty() ? null : className.trim());
        // 学生无学科；教师的学科默认空，可在后台管理系统中由管理员分配
        user.setSubject("TEACHER".equals(role) && subject != null && !subject.isEmpty() ? subject.trim() : null);
        userMapper.insert(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        return Result.ok(data);
    }
}
