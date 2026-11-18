package com.mind.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mind.agent.entity.DialogSession;
import com.mind.agent.entity.Faq;
import com.mind.agent.mapper.DialogSessionMapper;
import com.mind.agent.mapper.FaqMapper;
import com.mind.analysis.entity.PracticeRecord;
import com.mind.analysis.entity.StudentProfile;
import com.mind.analysis.mapper.PracticeRecordMapper;
import com.mind.analysis.mapper.StudentProfileMapper;
import com.mind.common.Result;
import com.mind.knowledge.entity.EduDocument;
import com.mind.knowledge.entity.QuestionBank;
import com.mind.knowledge.mapper.DocumentMapper;
import com.mind.knowledge.mapper.QuestionBankMapper;
import com.mind.mobile.entity.*;
import com.mind.mobile.mapper.*;
import com.mind.system.entity.SysUser;
import com.mind.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 后台管理系统接口：直接操纵 MySQL 业务库（mind_empower）。
 * 独立于教师端/学生端/移动端接口，仅 ADMIN 角色可访问。
 * 对应前端项目 admin-console（独立页面、独立登录）。
 */
@RestController
@RequestMapping("/api/dbadmin")
public class AdminDbController {

    private final UserMapper userMapper;
    private final MobileMeetingMapper meetingMapper;
    private final MobileMeetingMemberMapper memberMapper;
    private final MobileAnnotationMapper annotationMapper;
    private final MobileChatMessageMapper chatMessageMapper;
    private final MobileVoteMapper voteMapper;
    private final MobileVoteRecordMapper voteRecordMapper;
    private final MobileSummaryMapper summaryMapper;
    private final MobileTaskMapper taskMapper;
    private final MobileWrongQuestionMapper wrongQuestionMapper;
    private final MobilePlanLinkMapper planLinkMapper;
    private final QuestionBankMapper questionBankMapper;
    private final DocumentMapper documentMapper;
    private final FaqMapper faqMapper;
    private final DialogSessionMapper dialogSessionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final StudentProfileMapper studentProfileMapper;

    public AdminDbController(UserMapper userMapper,
                             MobileMeetingMapper meetingMapper,
                             MobileMeetingMemberMapper memberMapper,
                             MobileAnnotationMapper annotationMapper,
                             MobileChatMessageMapper chatMessageMapper,
                             MobileVoteMapper voteMapper,
                             MobileVoteRecordMapper voteRecordMapper,
                             MobileSummaryMapper summaryMapper,
                             MobileTaskMapper taskMapper,
                             MobileWrongQuestionMapper wrongQuestionMapper,
                             MobilePlanLinkMapper planLinkMapper,
                             QuestionBankMapper questionBankMapper,
                             DocumentMapper documentMapper,
                             FaqMapper faqMapper,
                             DialogSessionMapper dialogSessionMapper,
                             PracticeRecordMapper practiceRecordMapper,
                             StudentProfileMapper studentProfileMapper) {
        this.userMapper = userMapper;
        this.meetingMapper = meetingMapper;
        this.memberMapper = memberMapper;
        this.annotationMapper = annotationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.voteMapper = voteMapper;
        this.voteRecordMapper = voteRecordMapper;
        this.summaryMapper = summaryMapper;
        this.taskMapper = taskMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.planLinkMapper = planLinkMapper;
        this.questionBankMapper = questionBankMapper;
        this.documentMapper = documentMapper;
        this.faqMapper = faqMapper;
        this.dialogSessionMapper = dialogSessionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    // ---------- 权限：所有接口仅 ADMIN 可用 ----------

    private Result<?> denyIfNotAdmin() {
        if (!StpUtil.isLogin()) return Result.error(401, "未登录");
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) return Result.error(403, "仅管理员可访问后台管理系统");
        return null;
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- 数据总览：每张表多少行 ----------

    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("sys_user（用户）", userMapper.selectCount(null));
        m.put("edu_document（教学文档）", documentMapper.selectCount(null));
        m.put("question_bank（题库）", questionBankMapper.selectCount(null));
        m.put("mobile_meeting（会议）", meetingMapper.selectCount(null));
        m.put("mobile_meeting_member（会议成员）", memberMapper.selectCount(null));
        m.put("mobile_annotation（手写批注）", annotationMapper.selectCount(null));
        m.put("mobile_chat_message（会议聊天）", chatMessageMapper.selectCount(null));
        m.put("mobile_vote（投票）", voteMapper.selectCount(null));
        m.put("mobile_vote_record（投票记录）", voteRecordMapper.selectCount(null));
        m.put("mobile_summary（会议纪要）", summaryMapper.selectCount(null));
        m.put("mobile_task（会后任务）", taskMapper.selectCount(null));
        m.put("mobile_wrong_question（错题本）", wrongQuestionMapper.selectCount(null));
        m.put("mobile_plan_link（教案-题目关联）", planLinkMapper.selectCount(null));
        m.put("faq（高频问题）", faqMapper.selectCount(null));
        m.put("dialog_session（AI对话会话）", dialogSessionMapper.selectCount(null));
        m.put("practice_record（练习记录）", practiceRecordMapper.selectCount(null));
        m.put("student_profile（学情画像）", studentProfileMapper.selectCount(null));
        return Result.ok(m);
    }

    // ---------- 用户管理 ----------

    @GetMapping("/users")
    public Result<List<SysUser>> users(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("username", keyword).or().like("real_name", keyword));
        }
        if (role != null && !role.isEmpty()) qw.eq("role", role);
        qw.orderByAsc("id");
        return Result.ok(userMapper.selectList(qw));
    }

    @PostMapping("/users")
    public Result<?> addUser(@RequestBody SysUser body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        if (body.getUsername() == null || body.getUsername().trim().length() < 3)
            return Result.error("用户名需为 3~50 个字符");
        if (body.getPassword() == null || body.getPassword().length() < 6)
            return Result.error("密码至少 6 位");
        Long exists = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", body.getUsername().trim()));
        if (exists != null && exists > 0) return Result.error("该用户名已存在");
        SysUser u = new SysUser();
        u.setUsername(body.getUsername().trim());
        u.setPassword(body.getPassword().length() == 64 ? body.getPassword() : sha256(body.getPassword()));
        u.setRealName(body.getRealName());
        u.setRole(body.getRole() == null ? "STUDENT" : body.getRole());
        u.setClassName(body.getClassName());
        u.setSubject(body.getSubject());
        userMapper.insert(u);
        return Result.ok();
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody SysUser body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        SysUser me = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (me.getId().equals(id) && body.getRole() != null && !"ADMIN".equals(body.getRole()))
            return Result.error("不能取消自己的管理员角色");
        SysUser u = new SysUser();
        u.setId(id);
        u.setRealName(body.getRealName());
        u.setRole(body.getRole());
        u.setClassName(body.getClassName());
        u.setSubject(body.getSubject());
        if (body.getPassword() != null && !body.getPassword().isEmpty()) {
            if (body.getPassword().length() < 6) return Result.error("密码至少 6 位");
            u.setPassword(body.getPassword().length() == 64 ? body.getPassword() : sha256(body.getPassword()));
        }
        userMapper.updateById(u);
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        if (StpUtil.getLoginIdAsLong() == id) return Result.error("不能删除当前登录的管理员自己");
        SysUser target = userMapper.selectById(id);
        if (target != null && "admin".equals(target.getUsername())) return Result.error("内置管理员账号不允许删除");
        userMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 会议管理 ----------

    @GetMapping("/meetings")
    public Result<List<MobileMeeting>> meetings(@RequestParam(required = false) String keyword) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileMeeting> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) qw.like("title", keyword).or().like("meeting_no", keyword);
        qw.orderByDesc("id");
        return Result.ok(meetingMapper.selectList(qw));
    }

    @PutMapping("/meetings/{id}")
    public Result<?> updateMeeting(@PathVariable Long id, @RequestBody MobileMeeting body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        MobileMeeting m = new MobileMeeting();
        m.setId(id);
        m.setTitle(body.getTitle());
        m.setSubject(body.getSubject());
        m.setAgenda(body.getAgenda());
        m.setStatus(body.getStatus());
        m.setLocked(body.getLocked());
        meetingMapper.updateById(m);
        return Result.ok();
    }

    @DeleteMapping("/meetings/{id}")
    public Result<?> deleteMeeting(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        meetingMapper.deleteById(id);
        memberMapper.delete(new QueryWrapper<MobileMeetingMember>().eq("meeting_id", id));
        annotationMapper.delete(new QueryWrapper<MobileAnnotation>().eq("meeting_id", id));
        chatMessageMapper.delete(new QueryWrapper<MobileChatMessage>().eq("meeting_id", id));
        voteMapper.delete(new QueryWrapper<MobileVote>().eq("meeting_id", id));
        voteRecordMapper.delete(new QueryWrapper<MobileVoteRecord>().eq("meeting_id", id));
        summaryMapper.delete(new QueryWrapper<MobileSummary>().eq("meeting_id", id));
        taskMapper.delete(new QueryWrapper<MobileTask>().eq("meeting_id", id));
        return Result.ok();
    }

    @GetMapping("/meetings/{id}/members")
    public Result<List<MobileMeetingMember>> members(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        return Result.ok(memberMapper.selectList(new QueryWrapper<MobileMeetingMember>().eq("meeting_id", id)));
    }

    @DeleteMapping("/members/{id}")
    public Result<?> deleteMember(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        memberMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 题库管理 ----------

    @GetMapping("/questions")
    public Result<List<QuestionBank>> questions(@RequestParam(required = false) String keyword) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<QuestionBank> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) qw.like("content", keyword).or().like("concept_name", keyword);
        qw.orderByDesc("id");
        return Result.ok(questionBankMapper.selectList(qw));
    }

    @PostMapping("/questions")
    public Result<?> addQuestion(@RequestBody QuestionBank body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        body.setId(null);
        questionBankMapper.insert(body);
        return Result.ok();
    }

    @PutMapping("/questions/{id}")
    public Result<?> updateQuestion(@PathVariable Long id, @RequestBody QuestionBank body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        body.setId(id);
        questionBankMapper.updateById(body);
        return Result.ok();
    }

    @DeleteMapping("/questions/{id}")
    public Result<?> deleteQuestion(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        questionBankMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 错题本 ----------

    @GetMapping("/wrong-questions")
    public Result<List<MobileWrongQuestion>> wrongQuestions(@RequestParam(required = false) String keyword) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileWrongQuestion> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) qw.like("content", keyword).or().like("concept_name", keyword);
        qw.orderByDesc("id");
        return Result.ok(wrongQuestionMapper.selectList(qw));
    }

    @PostMapping("/wrong-questions")
    public Result<?> addWrongQuestion(@RequestBody MobileWrongQuestion body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        body.setId(null);
        wrongQuestionMapper.insert(body);
        return Result.ok();
    }

    @PutMapping("/wrong-questions/{id}")
    public Result<?> updateWrongQuestion(@PathVariable Long id, @RequestBody MobileWrongQuestion body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        body.setId(id);
        wrongQuestionMapper.updateById(body);
        return Result.ok();
    }

    @DeleteMapping("/wrong-questions/{id}")
    public Result<?> deleteWrongQuestion(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        wrongQuestionMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 教学文档 ----------

    @GetMapping("/documents")
    public Result<List<EduDocument>> documents(@RequestParam(required = false) String keyword) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<EduDocument> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) qw.like("title", keyword);
        qw.orderByDesc("id");
        return Result.ok(documentMapper.selectList(qw));
    }

    @DeleteMapping("/documents/{id}")
    public Result<?> deleteDocument(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        documentMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 会议过程数据：批注 / 聊天 / 投票 / 纪要 / 任务 ----------

    @GetMapping("/annotations")
    public Result<List<MobileAnnotation>> annotations(@RequestParam(required = false) Long meetingId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileAnnotation> qw = new QueryWrapper<>();
        if (meetingId != null) qw.eq("meeting_id", meetingId);
        qw.orderByDesc("id");
        return Result.ok(annotationMapper.selectList(qw));
    }

    @DeleteMapping("/annotations/{id}")
    public Result<?> deleteAnnotation(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        annotationMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/chat-messages")
    public Result<List<MobileChatMessage>> chatMessages(@RequestParam(required = false) Long meetingId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileChatMessage> qw = new QueryWrapper<>();
        if (meetingId != null) qw.eq("meeting_id", meetingId);
        qw.orderByDesc("id");
        return Result.ok(chatMessageMapper.selectList(qw));
    }

    @DeleteMapping("/chat-messages/{id}")
    public Result<?> deleteChatMessage(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        chatMessageMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/votes")
    public Result<List<MobileVote>> votes(@RequestParam(required = false) Long meetingId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileVote> qw = new QueryWrapper<>();
        if (meetingId != null) qw.eq("meeting_id", meetingId);
        qw.orderByDesc("id");
        return Result.ok(voteMapper.selectList(qw));
    }

    @GetMapping("/votes/{id}/records")
    public Result<List<MobileVoteRecord>> voteRecords(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        return Result.ok(voteRecordMapper.selectList(new QueryWrapper<MobileVoteRecord>().eq("vote_id", id)));
    }

    @DeleteMapping("/votes/{id}")
    public Result<?> deleteVote(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        voteMapper.deleteById(id);
        voteRecordMapper.delete(new QueryWrapper<MobileVoteRecord>().eq("vote_id", id));
        return Result.ok();
    }

    @GetMapping("/summaries")
    public Result<List<MobileSummary>> summaries(@RequestParam(required = false) Long meetingId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileSummary> qw = new QueryWrapper<>();
        if (meetingId != null) qw.eq("meeting_id", meetingId);
        qw.orderByDesc("id");
        return Result.ok(summaryMapper.selectList(qw));
    }

    @DeleteMapping("/summaries/{id}")
    public Result<?> deleteSummary(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        summaryMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/tasks")
    public Result<List<MobileTask>> tasks(@RequestParam(required = false) Long meetingId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<MobileTask> qw = new QueryWrapper<>();
        if (meetingId != null) qw.eq("meeting_id", meetingId);
        qw.orderByDesc("id");
        return Result.ok(taskMapper.selectList(qw));
    }

    @PutMapping("/tasks/{id}")
    public Result<?> updateTask(@PathVariable Long id, @RequestBody MobileTask body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        MobileTask t = new MobileTask();
        t.setId(id);
        t.setTitle(body.getTitle());
        t.setAssigneeId(body.getAssigneeId());
        t.setDeadline(body.getDeadline());
        t.setStatus(body.getStatus());
        taskMapper.updateById(t);
        return Result.ok();
    }

    @DeleteMapping("/tasks/{id}")
    public Result<?> deleteTask(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        taskMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 高频问题 / AI 对话 / 练习记录（只读 + 删除） ----------

    @GetMapping("/faqs")
    public Result<List<Faq>> faqs(@RequestParam(required = false) String keyword) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<Faq> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) qw.like("question_text", keyword);
        qw.orderByDesc("id");
        return Result.ok(faqMapper.selectList(qw));
    }

    @PutMapping("/faqs/{id}")
    public Result<?> updateFaq(@PathVariable Long id, @RequestBody Faq body) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        Faq f = new Faq();
        f.setId(id);
        f.setAnswer(body.getAnswer());
        f.setPublishStatus(body.getPublishStatus());
        faqMapper.updateById(f);
        return Result.ok();
    }

    @DeleteMapping("/faqs/{id}")
    public Result<?> deleteFaq(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        faqMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/dialog-sessions")
    public Result<List<DialogSession>> dialogSessions(@RequestParam(required = false) Long studentId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<DialogSession> qw = new QueryWrapper<>();
        if (studentId != null) qw.eq("student_id", studentId);
        qw.orderByDesc("id");
        return Result.ok(dialogSessionMapper.selectList(qw));
    }

    @DeleteMapping("/dialog-sessions/{id}")
    public Result<?> deleteDialogSession(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        dialogSessionMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/practice-records")
    public Result<List<PracticeRecord>> practiceRecords(@RequestParam(required = false) Long studentId) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        QueryWrapper<PracticeRecord> qw = new QueryWrapper<>();
        if (studentId != null) qw.eq("student_id", studentId);
        qw.orderByDesc("id");
        return Result.ok(practiceRecordMapper.selectList(qw));
    }

    @DeleteMapping("/practice-records/{id}")
    public Result<?> deletePracticeRecord(@PathVariable Long id) {
        Result<?> deny = denyIfNotAdmin();
        if (deny != null) return Result.error(deny.getCode(), deny.getMsg());
        practiceRecordMapper.deleteById(id);
        return Result.ok();
    }
}
