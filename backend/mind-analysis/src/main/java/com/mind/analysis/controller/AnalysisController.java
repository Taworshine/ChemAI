package com.mind.analysis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    /** 班级知识点覆盖热力图：学生 × footprint 的覆盖矩阵（已掌握=1.0，未掌握=0.0） */
    @GetMapping("/class/heatmap")
    public Result<Map<String, Object>> heatmap() {
        List<SysUser> students = userMapper.selectList(new QueryWrapper<SysUser>().eq("role", "STUDENT"));
        // 每个学生已掌握的 footprint 集合
        Map<Long, Set<String>> studentFootprints = new HashMap<>();
        Set<String> footprintSet = new LinkedHashSet<>();
        List<StudentProfile> all = profileMapper.selectList(null);
        for (StudentProfile p : all) {
            if (p.getFootprint() == null) continue;
            studentFootprints.computeIfAbsent(p.getStudentId(), k -> new LinkedHashSet<>()).add(p.getFootprint());
            footprintSet.add(p.getFootprint());
        }

        List<String> names = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        List<Map<String, Object>> profiles = new ArrayList<>();
        for (SysUser s : students) {
            names.add(s.getRealName() + "(" + s.getUsername() + ")");
            ids.add(s.getId());
            Set<String> fs = studentFootprints.getOrDefault(s.getId(), Collections.emptySet());
            Map<String, Object> row = new HashMap<>();
            for (String f : footprintSet) {
                row.put(f, fs.contains(f) ? 1.0 : 0.0);
            }
            profiles.add(row);
        }

        List<String> concepts = new ArrayList<>(footprintSet);
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

    /** 个体学情画像：已掌握知识点列表 + 掌握度 JSON（前端概念掌握度用） */
    @GetMapping("/student/{id}/radar")
    public Result<Map<String, Object>> radar(@PathVariable("id") Long id) {
        List<StudentProfile> profiles = profileMapper.selectList(
                new QueryWrapper<StudentProfile>().eq("student_id", id).orderByDesc("update_time"));
        Map<String, Object> mastery = new LinkedHashMap<>();
        List<Map<String, Object>> footprints = new ArrayList<>();
        for (StudentProfile p : profiles) {
            if (p.getFootprint() != null) {
                mastery.put(p.getFootprint(), 1.0);
                Map<String, Object> item = new HashMap<>();
                item.put("footprint", p.getFootprint());
                item.put("comment", p.getComment());
                item.put("updateTime", p.getUpdateTime());
                footprints.add(item);
            }
        }
        SysUser user = userMapper.selectById(id);
        Map<String, Object> data = new HashMap<>();
        // 新表结构无五维雷达，返回空维度，前端雷达图不再显示
        data.put("dimensions", new ArrayList<>());
        data.put("values", new ArrayList<>());
        data.put("realName", user == null ? "" : user.getRealName());
        try {
            data.put("conceptMastery", objectMapper.writeValueAsString(mastery));
        } catch (Exception e) {
            data.put("conceptMastery", "{}");
        }
        data.put("footprints", footprints);
        return Result.ok(data);
    }
}
