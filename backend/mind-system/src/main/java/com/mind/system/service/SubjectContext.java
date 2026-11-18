package com.mind.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 学科上下文：读取当前登录用户的角色与所属学科。
 * 教师只能看到/录入本学科内容；管理员（subject 为 null）不受限。
 */
@Component
public class SubjectContext {

    /** 高中学科字典（与管理员后台分配可选项保持一致） */
    public static final String[] SUBJECTS = {
            "语文", "数学", "英语", "物理", "化学", "生物", "历史", "地理", "政治"
    };

    private final UserMapper userMapper;

    public SubjectContext(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SysUser current() {
        return userMapper.selectById(StpUtil.getLoginIdAsLong());
    }

    public boolean isAdmin() {
        SysUser u = current();
        return u != null && "ADMIN".equals(u.getRole());
    }

    /** 当前教师所属学科；管理员返回 null（表示不限制） */
    public String currentSubject() {
        SysUser u = current();
        if (u == null || "ADMIN".equals(u.getRole())) {
            return null;
        }
        return u.getSubject();
    }
}
