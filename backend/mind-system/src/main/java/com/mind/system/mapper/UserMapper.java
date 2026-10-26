package com.mind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mind.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
