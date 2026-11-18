package com.mind.mobile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.EduDocument;
import com.mind.knowledge.mapper.DocumentMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教案量化评审：AI 解析结果与 Neo4j 图谱比对
 * 知识覆盖率 / 思维路径完整性 / 重难点预判
 */
@RestController
@RequestMapping("/api/mobile/review")
public class ReviewController {

    private final DocumentMapper documentMapper;
    private final Driver driver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewController(DocumentMapper documentMapper, Driver driver) {
        this.documentMapper = documentMapper;
        this.driver = driver;
    }

    /**
     * 分析某份教案的图谱覆盖情况。
     * docId = edu_document.id，要求 parse_status=2（已解析出 concepts/questionTypes/thinkingPaths/misconceptions）
     */
    @GetMapping("/analyze/{docId}")
    public Result<Map<String, Object>> analyze(@PathVariable("docId") Long docId) throws Exception {
        EduDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            return Result.error(400, "文档不存在");
        }
        if (doc.getParseResult() == null) {
            return Result.error(400, "文档尚未解析，请先调用 /api/document/parse/" + docId);
        }
        JsonNode extract = objectMapper.readTree(doc.getParseResult());

        List<String> docConcepts = toList(extract.get("concepts"));
        List<String> docPaths = toList(extract.get("thinkingPaths"))
                .stream().map(p -> p.contains(":") ? p.substring(0, p.indexOf(':')) : p)
                .collect(Collectors.toList());
        List<String> docMisconceptions = toList(extract.get("misconceptions"));

        // 图谱基准：按学科取全部 Concept / ThinkingPath / Misconception
        List<String> graphConcepts = queryNames(
                "MATCH (n:Concept) WHERE $s = '' OR n.subject = $s RETURN n.name AS name",
                doc.getTitle() == null ? "" : guessSubject(doc.getTitle()));
        List<String> graphPaths = queryNames(
                "MATCH (n:ThinkingPath) RETURN n.description AS name", "");
        List<String> graphMisconceptions = queryNames(
                "MATCH (n:Misconception) RETURN n.description AS name", "");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("docConcepts", docConcepts);
        data.put("conceptCoverage", coverage(docConcepts, graphConcepts));
        data.put("pathCoverage", coverage(docPaths, graphPaths));
        data.put("misconceptionCoverage", coverage(docMisconceptions, graphMisconceptions));
        return Result.ok(data);
    }

    /** 覆盖率明细：covered = 文档中命中图谱的内容，missing = 图谱有但教案未覆盖（前10条，供改进参考） */
    private Map<String, Object> coverage(List<String> docItems, List<String> graphItems) {
        Set<String> lower = docItems.stream().map(s -> s.toLowerCase()).collect(Collectors.toSet());
        List<String> covered = graphItems.stream()
                .filter(g -> lower.stream().anyMatch(d -> g.toLowerCase().contains(d) || d.contains(g.toLowerCase())))
                .collect(Collectors.toList());
        List<String> missing = graphItems.stream()
                .filter(g -> !covered.contains(g))
                .limit(10).collect(Collectors.toList());
        int percent = graphItems.isEmpty() ? 100
                : (int) Math.round(covered.size() * 100.0 / graphItems.size());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("percent", percent);
        item.put("covered", covered);
        item.put("missing", missing);
        return item;
    }

    private List<String> queryNames(String cypher, String subject) {
        List<String> names = new ArrayList<>();
        try (Session session = driver.session()) {
            session.run(cypher, org.neo4j.driver.Values.parameters("s", subject))
                    .forEachRemaining(r -> names.add(r.get("name").asString()));
        }
        return names;
    }

    private List<String> toList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        node.forEach(n -> {
            if (n.isTextual()) {
                list.add(n.asText());
            } else if (n.has("name")) {
                list.add(n.get("name").asText());
            }
        });
        return list;
    }

    /** 简易学科推断：会议/教案标题里出现学科名则按该学科过滤图谱 */
    private String guessSubject(String title) {
        for (String s : new String[]{"化学", "数学", "物理", "生物", "语文", "英语", "历史", "地理", "政治"}) {
            if (title.contains(s)) {
                return s;
            }
        }
        return "";
    }
}
