package com.mind.analysis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.analysis.entity.StudentProfile;
import com.mind.analysis.mapper.StudentProfileMapper;
import com.mind.common.Result;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学情分析：班级概念掌握度热力图 / 个体五维雷达图 / 学生列表
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final StudentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisController(StudentProfileMapper profileMapper, UserMapper userMapper) {
        this.profileMapper = profileMapper;
        this.userMapper = userMapper;
    }

    /** 班级学生列表（含姓名学号） */
    @GetMapping("/students")
    public Result<List<Map<String, Object>>> students() {
        List<SysUser> users = userMapper.selectList(new QueryWrapper<SysUser>().eq("role", "STUDENT"));
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("className", u.getClassName());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    /** 班级概念掌握度热力图：学生 × 概念 的掌握度矩阵 */
    @GetMapping("/class/heatmap")
    public Result<Map<String, Object>> heatmap() {
        List<SysUser> students = userMapper.selectList(new QueryWrapper<SysUser>().eq("role", "STUDENT"));
        List<Map<String, Object>> profiles = new ArrayList<>();
        Set<String> conceptSet = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (SysUser s : students) {
            StudentProfile p = profileMapper.selectOne(
                    new QueryWrapper<StudentProfile>().eq("student_id", s.getId()));
            names.add(s.getRealName() + "(" + s.getUsername() + ")");
            ids.add(s.getId());
            Map<String, Object> row = new HashMap<>();
            if (p != null && p.getConceptMastery() != null) {
                try {
                    JsonNode node = objectMapper.readTree(p.getConceptMastery());
                    node.properties().forEach(e -> {
                        conceptSet.add(e.getKey());
                        row.put(e.getKey(), e.getValue().asDouble());
                    });
                } catch (Exception ignored) {
                }
            }
            profiles.add(row);
        }

        List<String> concepts = new ArrayList<>(conceptSet);
        // 组装 ECharts heatmap data: [conceptIndex, studentIndex, value]
        List<int[]> heatmapData = new ArrayList<>();
        List<Object> flat = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            for (int j = 0; j < concepts.size(); j++) {
                Object v = profiles.get(i).get(concepts.get(j));
                double val = v == null ? 0 : Double.parseDouble(String.valueOf(v));
                flat.add(List.of(j, i, val));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("students", names);
        data.put("concepts", concepts);
        data.put("studentIds", ids);
        data.put("data", flat);
        return Result.ok(data);
    }

    /** 个体五维雷达图 */
    @GetMapping("/student/{id}/radar")
    public Result<Map<String, Object>> radar(@PathVariable("id") Long id) {
        StudentProfile p = profileMapper.selectOne(new QueryWrapper<StudentProfile>().eq("student_id", id));
        Map<String, Object> data = new HashMap<>();
        List<String> dims = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        if (p != null && p.getThinkingRadar() != null) {
            try {
                JsonNode node = objectMapper.readTree(p.getThinkingRadar());
                node.properties().forEach(e -> {
                    dims.add(e.getKey());
                    values.add(e.getValue().asInt());
                });
            } catch (Exception ignored) {
            }
        }
        SysUser user = userMapper.selectById(id);
        data.put("dimensions", dims);
        data.put("values", values);
        data.put("realName", user == null ? "" : user.getRealName());
        data.put("conceptMastery", p == null ? "{}" : p.getConceptMastery());
        return Result.ok(data);
    }
}
