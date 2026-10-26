package com.mind.knowledge.controller;

import com.mind.common.Result;
import com.mind.knowledge.entity.ExtractionTask;
import com.mind.knowledge.entity.SysUserRef;
import com.mind.knowledge.mapper.ExtractionTaskMapper;
import com.mind.knowledge.mapper.SysUserRefMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 知识图谱：读取完整图谱 / 新增节点 / 更新节点（含画布坐标）/ 删除节点 / 节点溯源
 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private static final String NODE_LABELS = "Concept|QuestionType|ThinkingPath|Misconception";

    private final Driver driver;
    private final ExtractionTaskMapper taskMapper;
    private final SysUserRefMapper userMapper;

    public GraphController(Driver driver, ExtractionTaskMapper taskMapper, SysUserRefMapper userMapper) {
        this.driver = driver;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取完整图谱（节点 + 边），供 G6 渲染。
     * scope=student 时只返回"已发布"内容：入库节点默认 DRAFT，教师发布后才对学生可见。
     */
    @GetMapping("/full")
    public Result<Map<String, Object>> full(@RequestParam(value = "scope", required = false) String scope) {
        boolean student = "student".equals(scope);
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        try (Session session = driver.session()) {
            // 节点
            String nodeQuery = "MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) RETURN n";
            if (student) {
                nodeQuery = "MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                        "AND coalesce(n.curation_status, 'PUBLISHED') = 'PUBLISHED' RETURN n";
            }
            session.run(nodeQuery)
                    .forEachRemaining(record -> {
                        Node node = record.get("n").asNode();
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", node.elementId());
                        item.put("label", node.labels().iterator().next());
                        Map<String, Object> props = new HashMap<>(node.asMap());
                        item.put("props", props);
                        nodes.add(item);
                    });
            // 边（端点必须也是图谱节点；学生端只看两个端点都已发布的边）
            String edgeQuery = "MATCH (a)-[r]->(b) WHERE any(l IN labels(a) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                            "AND any(l IN labels(b) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) RETURN r";
            if (student) {
                edgeQuery = "MATCH (a)-[r]->(b) WHERE any(l IN labels(a) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                        "AND any(l IN labels(b) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) " +
                        "AND coalesce(a.curation_status, 'PUBLISHED') = 'PUBLISHED' " +
                        "AND coalesce(b.curation_status, 'PUBLISHED') = 'PUBLISHED' RETURN r";
            }
            session.run(edgeQuery)
                    .forEachRemaining(record -> {
                        Relationship rel = record.get("r").asRelationship();
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rel.elementId());
                        item.put("type", rel.type());
                        item.put("source", rel.startNodeElementId());
                        item.put("target", rel.endNodeElementId());
                        item.put("props", new HashMap<>(rel.asMap()));
                        edges.add(item);
                    });
        }
        data.put("nodes", nodes);
        data.put("edges", edges);
        return Result.ok(data);
    }

    /**
     * 化学本体图谱：直接读取 Neo4j 中预置的高中化学本体数据。
     * 节点 = Entity（含 概念/物质/反应类型/化学量/化学式/电极/装置/实验/迷思概念/原理 等中文子类型）+ Section（教材章节）；
     * 边 = 本体关系（包含/属于/具有/反应生成/出自…，均为中文关系名）。
     * 前端「本体视图」的数据源：这 109 个实体 + 4 个章节 + 126 条关系全部来自你 Neo4j 里的真实数据。
     */
    @GetMapping("/ontology")
    public Result<Map<String, Object>> ontology() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        try (Session session = driver.session()) {
            session.run("MATCH (n:Entity) RETURN n").forEachRemaining(record -> {
                Node node = record.get("n").asNode();
                Map<String, Object> item = new HashMap<>();
                item.put("id", node.elementId());
                // 用中文子类型（概念/物质/…）作为 label，前端按它配颜色
                item.put("label", node.get("type").isNull() ? "Entity" : node.get("type").asString());
                item.put("props", new HashMap<>(node.asMap()));
                nodes.add(item);
            });
            session.run("MATCH (s:Section) RETURN s").forEachRemaining(record -> {
                Node node = record.get("s").asNode();
                Map<String, Object> item = new HashMap<>();
                item.put("id", node.elementId());
                item.put("label", "Section");
                item.put("props", new HashMap<>(node.asMap()));
                nodes.add(item);
            });
            session.run("MATCH (a)-[r]->(b) WHERE (a:Entity AND b:Entity) OR (a:Entity AND b:Section) RETURN r")
                    .forEachRemaining(record -> {
                        Relationship rel = record.get("r").asRelationship();
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rel.elementId());
                        item.put("type", rel.type());
                        item.put("source", rel.startNodeElementId());
                        item.put("target", rel.endNodeElementId());
                        item.put("props", new HashMap<>(rel.asMap()));
                        edges.add(item);
                    });
        }
        data.put("nodes", nodes);
        data.put("edges", edges);
        return Result.ok(data);
    }

    /**
     * Neo4j 服务端搜索：在图数据库里直接按名称/描述匹配（项目种子节点 + 化学本体实体），
     * 返回命中节点及其一跳邻居与关系，前端据此渲染结果子图并高亮命中节点。
     * 数据检索全部下推到 Neo4j 完成，前端不再本地过滤。
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam(value = "q", defaultValue = "") String q,
                                              @RequestParam(value = "scope", required = false) String scope) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        String kw = q.trim();
        data.put("nodes", nodes);
        data.put("edges", edges);
        data.put("matched", matched);
        if (kw.isEmpty()) {
            return Result.ok(data);
        }

        boolean student = "student".equals(scope);
        String pubFilter = student ? " AND coalesce(n.curation_status,'PUBLISHED') = 'PUBLISHED' " : "";
        String nodeFilter = "any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception','Entity'])" + pubFilter;
        try (Session session = driver.session()) {
            // 1) 命中节点（最多 50 个）
            org.neo4j.driver.Result result = session.run(
                    "MATCH (n) WHERE " + nodeFilter + " " +
                            "AND (toLower(coalesce(n.name,'')) CONTAINS toLower($kw) " +
                            " OR toLower(coalesce(n.description,'')) CONTAINS toLower($kw)) RETURN n LIMIT 50",
                    Values.parameters("kw", kw));
            List<String> ids = new ArrayList<>();
            Set<String> nodeIds = new HashSet<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node n = rec.get("n").asNode();
                ids.add(n.elementId());
                nodeIds.add(n.elementId());
                matched.add(n.elementId());
                nodes.add(toNodeItem(n));
            }
            if (ids.isEmpty()) {
                return Result.ok(data);
            }
            // 2) 命中节点的一跳邻居与关系（便于在图上定位上下文），去重后最多 400 条
            String neighborFilter = "any(l IN labels(o) WHERE l IN " +
                    "['Concept','QuestionType','ThinkingPath','Misconception','Entity','Section'])" +
                    (student ? " AND coalesce(o.curation_status,'PUBLISHED') = 'PUBLISHED' " : "");
            org.neo4j.driver.Result rel = session.run(
                    "MATCH (m)-[r]-(o) WHERE elementId(m) IN $ids AND " + neighborFilter +
                            "RETURN DISTINCT r, startNode(r) AS sn, endNode(r) AS en LIMIT 400",
                    Values.parameters("ids", ids));
            Set<String> edgeIds = new HashSet<>();
            while (rel.hasNext()) {
                Record rec = rel.next();
                Relationship r = rec.get("r").asRelationship();
                if (!edgeIds.add(r.elementId())) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.elementId());
                item.put("type", r.type());
                item.put("source", r.startNodeElementId());
                item.put("target", r.endNodeElementId());
                item.put("props", new HashMap<>(r.asMap()));
                edges.add(item);
                // 把邻居节点也放进结果（搜索视图要渲染它们）
                for (String key : new String[]{"sn", "en"}) {
                    Node end = rec.get(key).asNode();
                    if (nodeIds.add(end.elementId())) {
                        nodes.add(toNodeItem(end));
                    }
                }
            }
        }
        return Result.ok(data);
    }

    /** 统一的节点序列化：Entity 用中文子类型做 label，Section 用 Section，其余用种子标签 */
    private Map<String, Object> toNodeItem(Node node) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", node.elementId());
        String label;
        if (node.hasLabel("Entity")) {
            label = node.get("type").isNull() ? "Entity" : node.get("type").asString();
        } else if (node.hasLabel("Section")) {
            label = "Section";
        } else {
            label = node.labels().iterator().next();
        }
        item.put("label", label);
        item.put("props", new HashMap<>(node.asMap()));
        return item;
    }

    /** 新建节点：{label: 'Concept', props: {...}} */
    @PostMapping("/node")
    public Result<Map<String, Object>> createNode(@RequestBody Map<String, Object> body) {
        String label = String.valueOf(body.get("label"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) body.getOrDefault("props", new HashMap<>());
        if (!label.matches(NODE_LABELS)) {
            return Result.error("不支持的节点类型: " + label);
        }
        try (Session session = driver.session()) {
            Value result = session.run("CREATE (n:" + label + " $props) RETURN n", org.neo4j.driver.Values.parameters("props", props))
                    .single().get("n");
            Node node = result.asNode();
            Map<String, Object> item = new HashMap<>();
            item.put("id", node.elementId());
            item.put("label", label);
            item.put("props", new HashMap<>(node.asMap()));
            return Result.ok(item);
        }
    }

    /** 更新节点属性（如拖拽后的坐标 x/y、描述等） */
    @PutMapping("/node/{elementId}")
    public Result<Void> updateNode(@PathVariable("elementId") String elementId, @RequestBody Map<String, Object> props) {
        try (Session session = driver.session()) {
            session.run("MATCH (n) WHERE elementId(n) = $eid SET n += $props",
                    org.neo4j.driver.Values.parameters("eid", elementId, "props", props));
        }
        return Result.ok();
    }

    /** 删除节点及其关系 */
    @DeleteMapping("/node/{elementId}")
    public Result<Void> deleteNode(@PathVariable("elementId") String elementId) {
        try (Session session = driver.session()) {
            session.run("MATCH (n) WHERE elementId(n) = $eid DETACH DELETE n",
                    org.neo4j.driver.Values.parameters("eid", elementId));
        }
        return Result.ok();
    }

    /**
     * 节点溯源：点击图谱节点查看它来自哪份教案/讲义/知识点录入。
     * 返回节点属性 + 一跳关系摘要 + 入库任务信息（文档标题/类型/教师/时间/状态）。
     * 种子节点与化学本体节点没有 source_task_id，返回 sourceTask=null（前端显示"系统预置"）。
     */
    @GetMapping("/node/provenance")
    public Result<Map<String, Object>> provenance(@RequestParam("elementId") String elementId) {
        Map<String, Object> data = new HashMap<>();
        try (Session session = driver.session()) {
            org.neo4j.driver.Result r = session.run(
                    "MATCH (n) WHERE elementId(n) = $eid RETURN n, labels(n) AS ls", 
                    Values.parameters("eid", elementId));
            if (!r.hasNext()) return Result.error("节点不存在");
            Record rec = r.single();
            Node node = rec.get("n").asNode();
            Map<String, Object> props = new HashMap<>(node.asMap());
            data.put("props", props);
            data.put("labels", rec.get("ls").asList());

            // 一跳关系摘要（该节点与谁相连）
            List<Map<String, Object>> relations = new ArrayList<>();
            session.run("MATCH (n)-[rel]-(o) WHERE elementId(n) = $eid " +
                            "RETURN type(rel) AS t, startNode(rel) AS sn, endNode(rel) AS en LIMIT 30",
                    Values.parameters("eid", elementId)).forEachRemaining(rr -> {
                Node a = rr.get("sn").asNode();
                Node b = rr.get("en").asNode();
                boolean out = a.elementId().equals(elementId);
                Node other = out ? b : a;
                String otherName = other.get("name").isNull()
                        ? (other.get("description").isNull() ? "(未命名)" : other.get("description").asString())
                        : other.get("name").asString();
                Map<String, Object> m = new HashMap<>();
                m.put("direction", out ? "out" : "in");
                m.put("type", rr.get("t").asString());
                m.put("otherName", otherName.length() > 24 ? otherName.substring(0, 24) + "…" : otherName);
                relations.add(m);
            });
            data.put("relations", relations);

            // 来源任务（source_task_id → extraction_task + 教师姓名）
            Object tidObj = props.get("source_task_id");
            if (tidObj instanceof Number) {
                ExtractionTask task = taskMapper.selectById(((Number) tidObj).longValue());
                if (task != null) {
                    Map<String, Object> st = new HashMap<>();
                    st.put("taskId", task.getId());
                    st.put("title", task.getTitle());
                    st.put("docType", task.getDocType());
                    st.put("status", task.getStatus());
                    st.put("createdAt", task.getCreatedAt());
                    SysUserRef teacher = userMapper.selectById(task.getTeacherId());
                    st.put("teacherName", teacher == null ? null : teacher.getRealName());
                    data.put("sourceTask", st);
                }
            }
        }
        return Result.ok(data);
    }
}
