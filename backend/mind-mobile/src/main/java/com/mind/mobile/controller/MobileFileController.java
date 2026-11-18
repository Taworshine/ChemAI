package com.mind.mobile.controller;

import com.mind.common.Result;
import com.mind.knowledge.entity.EduDocument;
import com.mind.knowledge.mapper.DocumentMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 移动端文件服务：按文档 id 读取上传文件（图片/PDF），供 APP 预览与批注画布使用
 */
@RestController
@RequestMapping("/api/mobile/document")
public class MobileFileController {

    private static final Map<String, MediaType> MIME = new HashMap<>();

    static {
        MIME.put(".pdf", MediaType.APPLICATION_PDF);
        MIME.put(".png", MediaType.IMAGE_PNG);
        MIME.put(".jpg", MediaType.IMAGE_JPEG);
        MIME.put(".jpeg", MediaType.IMAGE_JPEG);
        MIME.put(".gif", MediaType.IMAGE_GIF);
        MIME.put(".txt", MediaType.TEXT_PLAIN);
        MIME.put(".doc", MediaType.valueOf("application/msword"));
        MIME.put(".docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    private final DocumentMapper documentMapper;
    private final com.mind.system.service.SubjectContext subjectContext;

    public MobileFileController(DocumentMapper documentMapper,
                                com.mind.system.service.SubjectContext subjectContext) {
        this.documentMapper = documentMapper;
        this.subjectContext = subjectContext;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable("id") Long id) throws Exception {
        EduDocument doc = documentMapper.selectById(id);
        if (doc == null || doc.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String name = path.getFileName().toString().toLowerCase();
        MediaType type = MIME.entrySet().stream()
                .filter(e -> name.endsWith(e.getKey())).map(Map.Entry::getValue)
                .findFirst().orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(Files.readAllBytes(path));
    }

    /** 教案库：我上传的全部教案（教案 Tab 列表）；教师仅见本学科 */
    @GetMapping("/library")
    public Result<java.util.List<Map<String, Object>>> library() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EduDocument> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EduDocument>()
                        .orderByDesc("id");
        String scope = subjectContext.currentSubject();
        if (scope != null) qw.eq("subject", scope);
        return Result.ok(documentMapper.selectList(qw).stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("title", d.getTitle());
            m.put("docType", d.getDocType());
            m.put("subject", d.getSubject());
            m.put("parseStatus", d.getParseStatus());
            m.put("createTime", d.getCreateTime());
            String name = d.getFilePath() == null ? "" : Paths.get(d.getFilePath()).getFileName().toString().toLowerCase();
            m.put("fileExt", name.contains(".") ? name.substring(name.lastIndexOf('.')) : "");
            return m;
        }).collect(java.util.stream.Collectors.toList()));
    }

    /** 供前端判断文档类型 */
    @GetMapping("/{id}/meta")
    public Result<Map<String, Object>> meta(@PathVariable("id") Long id) {
        EduDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", doc.getId());
        data.put("title", doc.getTitle());
        data.put("parseStatus", doc.getParseStatus());
        String name = doc.getFilePath() == null ? "" : Paths.get(doc.getFilePath()).getFileName().toString().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        data.put("fileExt", ext);
        data.put("isImage", ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".gif"));
        data.put("isPdf", ext.equals(".pdf"));
        data.put("isWord", ext.equals(".doc") || ext.equals(".docx"));
        return Result.ok(data);
    }
}
