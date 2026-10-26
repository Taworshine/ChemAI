package com.mind.knowledge.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind.common.Result;
import com.mind.common.ai.AiServiceClient;
import com.mind.knowledge.entity.EduDocument;
import com.mind.knowledge.mapper.DocumentMapper;
import com.mind.knowledge.mapper.ExtractionTaskMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 教学文档：上传 / 列表 / 触发解析
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentMapper documentMapper;
    private final ExtractionTaskMapper extractionTaskMapper;
    private final AiServiceClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String uploadDir;

    public DocumentController(DocumentMapper documentMapper,
                              ExtractionTaskMapper extractionTaskMapper,
                              AiServiceClient aiClient,
                              @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.extractionTaskMapper = extractionTaskMapper;
        this.aiClient = aiClient;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/upload")
    public Result<EduDocument> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("docType") String docType) throws Exception {
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        EduDocument doc = new EduDocument();
        doc.setTitle(original);
        doc.setDocType(docType);
        doc.setFilePath(target.toString());
        doc.setParseStatus(0);
        doc.setUploadUserId(StpUtil.getLoginIdAsLong());
        documentMapper.insert(doc);
        return Result.ok(doc);
    }

    @GetMapping("/list")
    public Result<List<EduDocument>> list() {
        return Result.ok(documentMapper.selectList(new QueryWrapper<EduDocument>().orderByDesc("id")));
    }

    /**
     * 删除文档（仅清理文件与记录；已被知识入库任务引用的文档拒绝删除，避免破坏溯源链）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        EduDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        Long taskCount = extractionTaskMapper.selectCount(
                new QueryWrapper<com.mind.knowledge.entity.ExtractionTask>().eq("doc_id", id));
        if (taskCount != null && taskCount > 0) {
            return Result.error("该文档已关联知识入库任务，请到「知识入库」页面管理");
        }
        if (doc.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (Exception ignored) {
                // 磁盘文件清理失败不阻断记录删除
            }
        }
        documentMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 调用 Python AI 服务解析文档：
     * 1. Java 把本地文件路径传给 Python；2. Python 用 pdfplumber / python-docx 提取文本；
     * 3. 调用 DeepSeek 抽取概念/题型/思维路径/卡点；4. 结果回写 MySQL。
     */
    @PostMapping("/parse/{id}")
    public Result<Map<String, Object>> parse(@PathVariable("id") Long id) {
        EduDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        doc.setParseStatus(1);
        documentMapper.updateById(doc);
        try {
            String text = extractTextLocally(doc.getFilePath());
            Map<String, Object> result = aiClient.extract(id, doc.getFilePath(), text);
            doc.setParseResult(objectMapper.writeValueAsString(result));
            doc.setParseStatus(2);
            documentMapper.updateById(doc);
            Map<String, Object> data = new HashMap<>();
            data.put("parseStatus", 2);
            data.put("extract", result);
            return Result.ok(data);
        } catch (Exception e) {
            doc.setParseStatus(3);
            documentMapper.updateById(doc);
            return Result.error("解析失败：" + e.getMessage());
        }
    }

    /** 纯文本文件直接在 Java 侧读取；PDF/Word 交给 Python 解析 */
    private String extractTextLocally(String filePath) {
        if (filePath == null) {
            return null;
        }
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
