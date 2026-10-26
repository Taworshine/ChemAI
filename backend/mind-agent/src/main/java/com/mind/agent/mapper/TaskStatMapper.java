package com.mind.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 跨模块直读 mind_empower.extraction_task（同库），只取教研看板所需统计 */
@Mapper
public interface TaskStatMapper {
    @Select("SELECT status, COUNT(*) AS c FROM extraction_task GROUP BY status")
    List<Map<String, Object>> statusCounts();

    @Select("SELECT COUNT(*) FROM extraction_item WHERE decision = 'PENDING'")
    Integer pendingItems();
}
