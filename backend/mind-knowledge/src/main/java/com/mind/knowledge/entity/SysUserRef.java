package com.mind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * sys_user 的只读引用（不含密码等敏感字段），供知识入库溯源显示教师姓名。
 * 不依赖 mind-system 模块，保持 mind-knowledge 轻量。
 */
@TableName("sys_user")
public class SysUserRef {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String realName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
}
