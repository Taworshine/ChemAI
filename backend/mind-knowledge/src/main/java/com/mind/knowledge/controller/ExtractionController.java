package com.mind.knowledge.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.common.Result;
import com.mind.common.ai.AiServiceClient;
import com.mind.knowledge.entity.ExtractionItem;
import com.mind.knowledge.entity.ExtractionTask;
import com.mind.knowledge.entity.IngestLog;
import com.mind.knowledge.mapper.ExtractionItemMapper;
import com.mind.knowledge.mapper.ExtractionTaskMapper;
import com.mind.knowledge.mapper.IngestLogMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 知识入库：上传（教案A/讲义B/知识点C）→ 大模型抽取 → 去重预匹配 → 教师确认 → MERGE 写入 Neo4j → 发布/下线。
 * 任何节点写入前必须经过教师确认（decision=NEW/MERGE/RELATE），防止图谱重复。
 */
@RestController
@RequestMapping("/api/extract")
public class ExtractionController {

    private final ExtractionTaskMapper taskMapper;
    private final ExtractionItemMapper itemMapper;
    private final IngestLogMapper logMapper;
    private final AiServiceClient aiClient;
    private final Driver driver;
    private final ObjectMapper om = new ObjectMapper();
    private final String uploadDir;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ExtractionController(ExtractionTaskMapper taskMapper, ExtractionItemMapper itemMapper,
                                IngestLogMapper logMapper, AiServiceClient aiClient, Driver driver,
                                @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.taskMapper = taskMapper;
        this.itemMapper = itemMapper;
        this.logMapper = logMapper;
        this.aiClient = aiClient;
        this.driver = driver;
        this.uploadDir = uploadDir;
    }

    /** 上传入口：A/B 传文件，C 传文本（也可传文件）。立即返回 task，后台流水线执行。 */
    @PostMapping("/upload")
    public Result<ExtractionTask> upload(@RequestParam("docType") String docType,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         @RequestParam(value = "text", required = false) String text,
                                         @RequestParam(value = "title", required = false) String title) throws Exception {
        ExtractionTask task = new ExtractionTask();
        task.setTeacherId(StpUtil.getLoginIdAsLong());
        task.setDocType(docType);
        task.setStatus("PARSING");
        task.setVersion(0);
        task.setTitle(title != null && !title.isBlank() ? title
                : (file != null ? file.getOriginalFilename() : "知识点录入 " + LocalDateTime.now().toLocalDate()));

        String filePath = null;
        if (file != null && !file.isEmpty()) {
            String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            filePath = target.toString();
        }
        // 纯文本直接在 Java 侧读出，PDF/Word 交给 Python 解析
        String localText = extractTextLocally(filePath);
        task.setDocText(text != null && !text.isBlank() ? text : localText);
        taskMapper.insert(task);

        final Long taskId = task.getId();
        final String fp = filePath;
        executor.execute(() -> runPipeline(taskId, fp));
        return Result.ok(task);
    }

    /** 教师的上传任务列表（含待裁决数） */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        List<ExtractionTask> list = taskMapper.selectList(
                new QueryWrapper<ExtractionTask>().eq("teacher_id", StpUtil.getLoginIdAsLong()).orderByDesc("id"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ExtractionTask t : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("task", t);
            m.put("pending", itemMapper.selectCount(new QueryWrapper<ExtractionItem>()
                    .eq("task_id", t.getId()).eq("decision", "PENDING")));
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/task/{id}")
    public Result<ExtractionTask> task(@PathVariable("id") Long id) {
        return Result.ok(taskMapper.selectById(id));
    }

    @GetMapping("/task/{id}/items")
    public Result<List<ExtractionItem>> items(@PathVariable("id") Long id,
                                              @RequestParam(value = "kind", required = false) String kind) {
        QueryWrapper<ExtractionItem> qw = new QueryWrapper<ExtractionItem>().eq("task_id", id).orderByAsc("id");
        if (kind != null && !kind.isBlank()) qw.eq("kind", kind);
        return Result.ok(itemMapper.selectList(qw));
    }

    /** 单条裁决 / 编辑（自动存草稿） */
    @PutMapping("/item/{id}")
    public Result<Void> decide(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        ExtractionItem item = itemMapper.selectById(id);
        if (item == null) return Result.error("条目不存在");
        if (body.get("decision") != null) item.setDecision(String.valueOf(body.get("decision")));
        if (body.get("payload") != null) item.setEditedPayload(String.valueOf(body.get("payload")));
        itemMapper.updateById(item);
        return Result.ok();
    }

    /** 手动补漏：教师在确认页直接添加抽取漏掉的知识点 */
    @PostMapping("/task/{id}/manual-item")
    public Result<ExtractionItem> manualItem(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        ExtractionItem item = new ExtractionItem();
        item.setTaskId(id);
        item.setKind(String.valueOf(body.getOrDefault("kind", "concept")));
        item.setPayload(String.valueOf(body.getOrDefault("payload", "{}")));
        item.setDecision("NEW");
        item.setMatchLevel(null);
        itemMapper.insert(item);
        return Result.ok(item);
    }

    /** 全部接受建议：L1 精确匹配→合并，其余→新建 */
    @PostMapping("/task/{id}/accept-all")
    public Result<Void> acceptAll(@PathVariable("id") Long id) {
        List<ExtractionItem> pendings = itemMapper.selectList(new QueryWrapper<ExtractionItem>()
                .eq("task_id", id).eq("decision", "PENDING"));
        for (ExtractionItem it : pendings) {
            it.setDecision("L1".equals(it.getMatchLevel()) ? "MERGE" : "NEW");
            itemMapper.updateById(it);
        }
        return Result.ok();
    }

    /** 提交确认：事务写入 Neo4j（MERGE 语义，天然防重复） */
    @PostMapping("/task/{id}/submit")
    public Result<Map<String, Object>> submit(@PathVariable("id") Long id) {
        ExtractionTask task = taskMapper.selectById(id);
        if (task == null) return Result.error("任务不存在");
        if (!"PENDING_CONFIRM".equals(task.getStatus()) && !"CONFIRMED".equals(task.getStatus())) {
            return Result.error("当前状态不可提交: " + task.getStatus());
        }
        List<ExtractionItem> items = itemMapper.selectList(new QueryWrapper<ExtractionItem>().eq("task_id", id));
        long pending = items.stream().filter(i -> "PENDING".equals(i.getDecision())).count();
        if (pending > 0) return Result.error("还有 " + pending + " 条未裁决，请先处理");

        Map<String, Object> stats;
        try {
            stats = writeToNeo4j(task, items);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setFailReason("写入 Neo4j 失败: " + e.getMessage());
            taskMapper.updateById(task);
            return Result.error(task.getFailReason());
        }
        task.setStatus("CONFIRMED");
        task.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        writeLog(id, "SUBMIT", stats);
        return Result.ok(new HashMap<String, Object>(stats));
    }

    /** 发布：该任务写入的节点对学生端可见 */
    @PostMapping("/task/{id}/publish")
    public Result<Void> publish(@PathVariable("id") Long id) {
        ExtractionTask task = taskMapper.selectById(id);
        if (task == null || !"CONFIRMED".equals(task.getStatus())) return Result.error("仅已确认任务可发布");
        try (Session s = driver.session()) {
            s.run("MATCH (n) WHERE n.source_task_id = $tid SET n.curation_status = 'PUBLISHED', n.version = $v",
                    Values.parameters("tid", id, "v", task.getVersion() + 1));
        }
        task.setStatus("PUBLISHED");
        task.setVersion(task.getVersion() + 1);
        taskMapper.updateById(task);
        writeLog(id, "PUBLISH", Map.of("version", task.getVersion()));
        return Result.ok();
    }

    /** 下线：学生端立即不可见，数据保留 */
    @PostMapping("/task/{id}/offline")
    public Result<Void> offline(@PathVariable("id") Long id) {
        ExtractionTask task = taskMapper.selectById(id);
        if (task == null || !"PUBLISHED".equals(task.getStatus())) return Result.error("仅已发布任务可下线");
        try (Session s = driver.session()) {
            s.run("MATCH (n) WHERE n.source_task_id = $tid SET n.curation_status = 'OFFLINE'",
                    Values.parameters("tid", id));
        }
        task.setStatus("OFFLINE");
        taskMapper.updateById(task);
        writeLog(id, "OFFLINE", null);
        return Result.ok();
    }

    /**
     * 按任务回滚：删除该任务"新建"的节点（提交时精确记录的 elementId 清单），
     * 被合并的已有节点和种子/本体节点一律保留。
     * 只有 created_node_ids 清单存在的任务才能回滚（旧任务没有清单，拒绝以保证安全）。
     */
    @PostMapping("/task/{id}/rollback")
    public Result<Map<String, Object>> rollback(@PathVariable("id") Long id) {
        ExtractionTask task = taskMapper.selectById(id);
        if (task == null) return Result.error("任务不存在");
        String status = task.getStatus();
        if (!"CONFIRMED".equals(status) && !"PUBLISHED".equals(status) && !"OFFLINE".equals(status)) {
            return Result.error("当前状态不可回滚: " + status + "（仅 已入库/已发布/已下线 可回滚）");
        }
        if (task.getCreatedNodeIds() == null || task.getCreatedNodeIds().isBlank()) {
            return Result.error("该任务没有记录新建节点清单（旧数据），为避免误删已拒绝回滚");
        }
        List<String> ids;
        try {
            ids = om.readValue(task.getCreatedNodeIds(), List.class);
        } catch (Exception e) {
            return Result.error("新建节点清单解析失败: " + e.getMessage());
        }
        int deleted;
        try (Session s = driver.session()) {
            deleted = s.writeTransaction(tx -> {
                int d = 0;
                for (String eid : ids) {
                    // 双重保护：必须命中清单 elementId 且 source_task_id 对得上才删
                    d += tx.run("MATCH (n) WHERE elementId(n) = $eid AND n.source_task_id = $tid " +
                                            "DETACH DELETE n RETURN count(*) AS c",
                                    Values.parameters("eid", eid, "tid", id))
                            .single().get("c").asInt();
                }
                return d;
            });
        } catch (Exception e) {
            return Result.error("回滚失败: " + e.getMessage());
        }
        task.setStatus("ROLLED_BACK");
        taskMapper.updateById(task);
        Map<String, Object> detail = new HashMap<>();
        detail.put("deleted", deleted);
        detail.put("recorded", ids.size());
        writeLog(id, "ROLLBACK", detail);
        return Result.ok(detail);
    }

    // ================================================================
    //                        内部流水线
    // ================================================================

    private void runPipeline(Long taskId, String filePath) {
        ExtractionTask task = taskMapper.selectById(taskId);
        try {
            // 1) 大模型抽取
            task.setStatus("EXTRACTING");
            taskMapper.updateById(task);
            Map<String, Object> result = aiClient.extract(taskId, task.getDocType(), filePath, task.getDocText());
            task.setResultJson(om.writeValueAsString(result));
            taskMapper.updateById(task);

            // 2) 去重预匹配（vs Neo4j 现有节点：种子四类 + 化学本体 Entity）
            task.setStatus("MATCHING");
            taskMapper.updateById(task);
            List<Map<String, Object>> existing = loadExistingNodes();
            List<ExtractionItem> items = new ArrayList<>();
            JsonNode root = om.readTree(task.getResultJson());
            collectItems(taskId, "concept", root.get("concepts"), existing, items);
            collectItems(taskId, "qtype", root.get("question_types"), existing, items);
            if (root.has("thinking_paths")) {
                for (JsonNode p : root.get("thinking_paths")) {
                    ExtractionItem item = new ExtractionItem();
                    item.setTaskId(taskId);
                    item.setKind("path");
                    item.setPayload(p.toString());
                    item.setDecision("PENDING");
                    items.add(item);
                }
            }
            collectItems(taskId, "misconception", root.get("misconceptions"), existing, items);
            for (ExtractionItem it : items) itemMapper.insert(it);

            task.setStatus("PENDING_CONFIRM");
            taskMapper.updateById(task);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setFailReason(e.getMessage());
            taskMapper.updateById(task);
        }
    }

    /** concept/qtype/misconception 候选 → 与现有节点匹配，生成待裁决条目 */
    private void collectItems(Long taskId, String kind, JsonNode arr,
                              List<Map<String, Object>> existing, List<ExtractionItem> out) {
        if (arr == null || !arr.isArray()) return;
        for (JsonNode n : arr) {
            ExtractionItem item = new ExtractionItem();
            item.setTaskId(taskId);
            item.setKind(kind);
            item.setPayload(n.toString());
            item.setDecision("PENDING");
            String name = n.hasNonNull("name") ? n.get("name").asText()
                    : (n.hasNonNull("description") ? n.get("description").asText() : "");
            String subject = n.hasNonNull("subject") ? n.get("subject").asText() : "";
            Map<String, Object> best = bestMatch(name, subject,
                    n.hasNonNull("keywords") ? n.get("keywords") : null, existing);
            if (best != null) {
                item.setMatchLevel((String) best.get("level"));
                item.setMatchNodeId((String) best.get("id"));
                item.setMatchNodeLabel((String) best.get("label"));
                item.setMatchNodeName((String) best.get("name"));
                item.setSimilarity((Double) best.get("similarity"));
            }
            out.add(item);
        }
    }

    /** 加载 Neo4j 现有可匹配节点（种子四类 + 化学本体 Entity） */
    private List<Map<String, Object>> loadExistingNodes() {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Session s = driver.session()) {
            s.run("MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','Entity']) " +
                    "RETURN elementId(n) AS id, labels(n) AS ls, n.name AS name, n.subject AS subject")
                    .forEachRemaining(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", r.get("id").asString());
                        String label = "Entity";
                        for (Object l : r.get("ls").asList()) {
                            if (!"Entity".equals(l)) { label = String.valueOf(l); break; }
                        }
                        m.put("label", label);
                        m.put("name", r.get("name").asString(null));
                        m.put("subject", r.get("subject").asString(null));
                        out.add(m);
                    });
        }
        return out;
    }

    /** 三级匹配：L1 精确(name+subject) → L2 模糊(包含/编辑距离/关键词 Jaccard)，返回最佳匹配 */
    private Map<String, Object> bestMatch(String name, String subject, JsonNode keywords,
                                          List<Map<String, Object>> existing) {
        if (name == null || name.isBlank()) return null;
        String lower = name.toLowerCase();
        Map<String, Object> best = null;
        double bestScore = 0;
        for (Map<String, Object> e : existing) {
            String en = (String) e.get("name");
            if (en == null) continue;
            String el = en.toLowerCase();
            // L1 精确
            if (el.equals(lower) && (subject == null || subject.isBlank()
                    || subject.equals(e.get("subject")) || e.get("subject") == null)) {
                Map<String, Object> m = new HashMap<>(e);
                m.put("level", "L1");
                m.put("similarity", 1.0);
                return m;   // 精确命中直接返回
            }
            double score = 0;
            if (el.contains(lower) || lower.contains(el)) score = 0.8;
            else if (levenshtein(lower, el) <= 2) score = 0.7;
            if (keywords != null && keywords.isArray()) {
                Set<String> ks = new HashSet<>();
                keywords.forEach(k -> ks.add(k.asText().toLowerCase()));
                if (!ks.isEmpty()) {
                    String[] es = en.split("[\\s,，、/]+");
                    Set<String> es2 = new HashSet<>(Arrays.asList(es));
                    es2.retainAll(ks);
                    if (!es2.isEmpty()) score = Math.max(score, 0.5 + 0.1 * es2.size());
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = new HashMap<>(e);
                best.put("level", "L2");
                best.put("similarity", Math.min(score, 0.99));
            }
        }
        return bestScore >= 0.5 ? best : null;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                cur[j] = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1),
                        prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
            prev = cur;
        }
        return prev[b.length()];
    }

    /**
     * 确认后事务写入 Neo4j：MERGE 节点、挂关系、全部带 source_task_id 溯源。
     * 溯源属性只在 ON CREATE 时写入——命中已有节点（种子/本体/其他任务建的）不会被抢注，
     * 保证回滚只删除本任务真正新建的节点。
     */
    private Map<String, Object> writeToNeo4j(ExtractionTask task, List<ExtractionItem> items) throws Exception {
        Long teacherId = task.getTeacherId();
        Map<String, Integer> stats = new HashMap<>();
        stats.put("newConcepts", 0); stats.put("mergeConcepts", 0);
        stats.put("newQtypes", 0); stats.put("paths", 0); stats.put("misconceptions", 0);
        List<String> createdIds = new ArrayList<>();
        List<String> createdNames = new ArrayList<>();   // 新建节点名，回传给前端做"去图谱看看"直达

        try (Session s = driver.session()) {
            s.writeTransaction(tx -> {
                for (ExtractionItem it : items) {
                    String payloadStr = it.getEditedPayload() != null ? it.getEditedPayload() : it.getPayload();
                    JsonNode p;
                    try { p = om.readTree(payloadStr); } catch (Exception ex) { continue; }
                    String dec = it.getDecision();

                    if ("concept".equals(it.getKind())) {
                        if ("NEW".equals(dec)) {
                            // ON CREATE：全新节点，写入溯源；ON MATCH：同名节点已存在，只补充描述/关键词，不抢注溯源
                            org.neo4j.driver.Result r = tx.run(
                                    "MERGE (c:Concept {name:$name, subject:$subject}) " +
                                            "ON CREATE SET c.description = $desc, c.keywords = $kw, c.grade = $grade, " +
                                            "c.source_task_id = $tid, c.source_doc_id = $tid, " +
                                            "c.created_by = $uid, c.created_at = datetime(), " +
                                            "c.curation_status = 'DRAFT' " +
                                            "ON MATCH SET c.description = coalesce($desc, c.description), " +
                                            "c.keywords = coalesce($kw, c.keywords), " +
                                            "c.grade = coalesce($grade, c.grade) " +
                                            "RETURN elementId(c) AS id, c.source_task_id = $tid AS created",
                                    Values.parameters(
                                            "name", text(p, "name"), "subject", text(p, "subject", "化学"),
                                            "desc", text(p, "description"), "kw", list(p, "keywords"),
                                            "grade", text(p, "grade"), "tid", task.getId(), "uid", teacherId));
                            org.neo4j.driver.Record rec = r.single();
                            if (rec.get("created").asBoolean()) { createdIds.add(rec.get("id").asString()); createdNames.add(text(p, "name")); }
                            stats.merge("newConcepts", 1, Integer::sum);
                        } else if ("MERGE".equals(dec) && it.getMatchNodeId() != null) {
                            // 合并到已有节点：只补充内容；source_task_id 不抢注（保留原始出处）
                            tx.run("MATCH (n) WHERE elementId(n) = $eid " +
                                            "SET n.description = coalesce($desc, n.description), " +
                                            "n.keywords = coalesce($kw, n.keywords), " +
                                            "n.source_task_id = coalesce(n.source_task_id, $tid)",
                                    Values.parameters("eid", it.getMatchNodeId(),
                                            "desc", text(p, "description"), "kw", list(p, "keywords"),
                                            "tid", task.getId()));
                            stats.merge("mergeConcepts", 1, Integer::sum);
                        } else if ("RELATE".equals(dec) && it.getMatchNodeId() != null) {
                            // 建前置：新节点挂 PREREQUISITE 指向已有节点（溯源只在 ON CREATE 写）
                            org.neo4j.driver.Result r = tx.run(
                                    "MERGE (c:Concept {name:$name, subject:$subject}) " +
                                            "ON CREATE SET c.source_task_id = $tid, c.created_by = $uid, " +
                                            "c.created_at = datetime(), c.curation_status = 'DRAFT' " +
                                            "WITH c MATCH (n) WHERE elementId(n) = $eid " +
                                            "MERGE (c)-[:PREREQUISITE]->(n) " +
                                            "RETURN elementId(c) AS id, c.source_task_id = $tid AS created",
                                    Values.parameters("name", text(p, "name"),
                                            "subject", text(p, "subject", "化学"),
                                            "tid", task.getId(), "uid", teacherId, "eid", it.getMatchNodeId()));
                            org.neo4j.driver.Record rec = r.single();
                            if (rec.get("created").asBoolean()) { createdIds.add(rec.get("id").asString()); createdNames.add(text(p, "name")); }
                            stats.merge("newConcepts", 1, Integer::sum);
                        }
                    } else if ("qtype".equals(it.getKind()) && "NEW".equals(dec)) {
                        org.neo4j.driver.Result r = tx.run(
                                "MERGE (q:QuestionType {name:$name}) " +
                                        "ON CREATE SET q.subject = $subject, q.difficulty = $diff, " +
                                        "q.typicalExample = $ex, q.source_task_id = $tid, q.created_by = $uid, " +
                                        "q.created_at = datetime(), q.curation_status = 'DRAFT' " +
                                        "ON MATCH SET q.subject = coalesce($subject, q.subject), " +
                                        "q.difficulty = coalesce($diff, q.difficulty), " +
                                        "q.typicalExample = coalesce($ex, q.typicalExample) " +
                                        "RETURN elementId(q) AS id, q.source_task_id = $tid AS created",
                                Values.parameters("name", text(p, "name"),
                                        "subject", text(p, "subject"), "diff", text(p, "difficulty"),
                                        "ex", text(p, "typical_example"), "tid", task.getId(), "uid", teacherId));
                        org.neo4j.driver.Record rec = r.single();
                        if (rec.get("created").asBoolean()) { createdIds.add(rec.get("id").asString()); createdNames.add(text(p, "name")); }
                        stats.merge("newQtypes", 1, Integer::sum);
                    } else if ("path".equals(it.getKind()) && ("NEW".equals(dec) || "MERGE".equals(dec))) {
                        // 思维路径：按 question_type 找/建题型节点，按 order 建步骤并挂 REQUIRES_PATH
                        String qtName = text(p, "question_type");
                        if (qtName == null || qtName.isBlank()) continue;
                        for (JsonNode step : p.has("steps") ? p.get("steps") : om.createArrayNode()) {
                            org.neo4j.driver.Result r = tx.run(
                                    "MERGE (q:QuestionType {name:$qn}) " +
                                            "ON CREATE SET q.source_task_id = $tid, q.curation_status = 'DRAFT' " +
                                            "CREATE (tp:ThinkingPath {description:$desc, hint:$hint, " +
                                            "stepOrder:$order, source_task_id:$tid, curation_status:'DRAFT'}) " +
                                            "CREATE (q)-[:REQUIRES_PATH]->(tp) " +
                                            "RETURN elementId(tp) AS id, true AS created",
                                    Values.parameters("qn", qtName, "tid", task.getId(),
                                            "desc", text(step, "description"), "hint", text(step, "hint"),
                                            "order", step.has("order") ? step.get("order").asInt() : 1));
                            createdIds.add(r.single().get("id").asString());
                            stats.merge("paths", 1, Integer::sum);
                        }
                    } else if ("misconception".equals(it.getKind()) && "NEW".equals(dec)) {
                        org.neo4j.driver.Result r = tx.run(
                                "CREATE (m:Misconception {description:$desc, relatedConcept:$rc, " +
                                        "frequency:$freq, source_task_id:$tid, curation_status:'DRAFT'}) " +
                                        "RETURN elementId(m) AS id, true AS created",
                                Values.parameters("desc", text(p, "description"),
                                        "rc", text(p, "related_concept"), "freq", text(p, "frequency", "mid"),
                                        "tid", task.getId()));
                        createdIds.add(r.single().get("id").asString());
                        stats.merge("misconceptions", 1, Integer::sum);
                    }
                }
                return null;
            });
        }
        // 新建节点清单落库：回滚按这份清单精确删除，绝不碰种子/本体/被合并节点
        task.setCreatedNodeIds(om.writeValueAsString(createdIds));
        taskMapper.updateById(task);
        // 计数 + 新建节点名一起返回（前端"写入成功"弹窗做直达图谱用）
        Map<String, Object> out = new HashMap<>(stats);
        out.put("createdNames", createdNames);
        return out;
    }

    private void writeLog(Long taskId, String action, Map<String, ?> detail) {
        IngestLog log = new IngestLog();
        log.setTaskId(taskId);
        log.setAction(action);
        log.setOperator(StpUtil.getLoginIdAsLong());
        try {
            log.setDetail(detail == null ? null : om.writeValueAsString(detail));
        } catch (Exception ignored) {
        }
        logMapper.insert(log);
    }

    private static String text(JsonNode n, String key) { return text(n, key, null); }

    private static String text(JsonNode n, String key, String def) {
        return n.hasNonNull(key) ? n.get(key).asText() : def;
    }

    private static List<String> list(JsonNode n, String key) {
        List<String> out = new ArrayList<>();
        if (n.has(key) && n.get(key).isArray()) n.get(key).forEach(k -> out.add(k.asText()));
        return out.isEmpty() ? null : out;
    }

    private String extractTextLocally(String filePath) {
        if (filePath == null) return null;
        File f = new File(filePath);
        String name = f.getName().toLowerCase();
        if (name.endsWith(".txt") || name.endsWith(".md")) {
            try {
                return Files.readString(f.toPath());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
