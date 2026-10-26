package com.mind.knowledge.controller;

import com.mind.common.Result;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 知识图谱后台管理 API：
 * 学科列表 / 节点分页搜索 / 关系（边）增删
 */
@RestController
@RequestMapping("/api/graph")
public class GraphAdminController {

    private static final String NODE_LABELS = "Concept|QuestionType|ThinkingPath|Misconception";
    private static final String REL_TYPES = "HAS_QUESTION_TYPE|REQUIRES_PATH|COMMON_MISCONCEPTION|PREREQUISITE";

    private final Driver driver;

    public GraphAdminController(Driver driver) {
        this.driver = driver;
    }

    /** 全部学科（去重） */
    @GetMapping("/subjects")
    public Result<List<String>> subjects() {
        List<String> list = new ArrayList<>();
        try (Session session = driver.session()) {
            session.run("MATCH (n) WHERE n:Concept OR n:QuestionType RETURN DISTINCT n.subject AS s ORDER BY s")
                    .forEachRemaining(r -> {
                        if (!r.get("s").isNull()) list.add(r.get("s").asString());
                    });
        }
        return Result.ok(list);
    }

    /** 节点分页搜索：label / subject / keyword */
    @GetMapping("/nodes")
    public Result<Map<String, Object>> nodes(@RequestParam(defaultValue = "Concept") String label,
                                               @RequestParam(required = false) String subject,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        if (!label.matches(NODE_LABELS)) {
            return Result.error("不支持的节点类型: " + label);
        }
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        long total;
        String where = " WHERE ($subject IS NULL OR n.subject = $subject) "
                + " AND ($keyword IS NULL OR toString(n.name) CONTAINS $keyword "
                + " OR toString(n.description) CONTAINS $keyword "
                + " OR toString(n.typicalExample) CONTAINS $keyword) ";
        try (Session session = driver.session()) {
            total = session.run("MATCH (n:" + label + ")" + where + " RETURN count(n) AS c",
                            org.neo4j.driver.Values.parameters(
                                    "subject", subject, "keyword", keyword))
                    .single().get("c").asLong();
            session.run("MATCH (n:" + label + ")" + where + " RETURN n ORDER BY coalesce(n.id, 9999), n.name "
                            + " SKIP $skip LIMIT $limit",
                            org.neo4j.driver.Values.parameters(
                                    "subject", subject, "keyword", keyword,
                                    "skip", (page - 1) * size, "limit", size))
                    .forEachRemaining(r -> {
                        Node n = r.get("n").asNode();
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", n.elementId());
                        item.put("label", n.labels().iterator().next());
                        item.put("props", new HashMap<>(n.asMap()));
                        list.add(item);
                    });
        }
        data.put("total", total);
        data.put("list", list);
        return Result.ok(data);
    }

    /** 新建关系：{source, target, type, props}（前端先后点击两个节点） */
    @PostMapping("/edge")
    public Result<Map<String, Object>> createEdge(@RequestBody Map<String, Object> body) {
        String source = String.valueOf(body.get("source"));
        String target = String.valueOf(body.get("target"));
        String type = String.valueOf(body.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) body.getOrDefault("props", new HashMap<>());
        if (!type.matches(REL_TYPES)) {
            return Result.error("不支持的关系类型: " + type);
        }
        try (Session session = driver.session()) {
            Value v = session.run("MATCH (a), (b) WHERE elementId(a) = $s AND elementId(b) = $t "
                                    + " CREATE (a)-[r:" + type + "]->(b) SET r += $props RETURN r",
                            org.neo4j.driver.Values.parameters("s", source, "t", target, "props", props))
                    .single().get("r");
            Relationship rel = v.asRelationship();
            Map<String, Object> item = new HashMap<>();
            item.put("id", rel.elementId());
            item.put("type", rel.type());
            item.put("source", rel.startNodeElementId());
            item.put("target", rel.endNodeElementId());
            return Result.ok(item);
        }
    }

    /** 删除关系 */
    @DeleteMapping("/edge/{elementId}")
    public Result<Void> deleteEdge(@PathVariable("elementId") String elementId) {
        try (Session session = driver.session()) {
            session.run("MATCH ()-[r]->() WHERE elementId(r) = $eid DELETE r",
                    org.neo4j.driver.Values.parameters("eid", elementId));
        }
        return Result.ok();
    }
}
