package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.CreateExamPaperRequest;
import com.example.onlineexam.dto.UpdateExamPaperRequest;
import com.example.onlineexam.dto.AutoAssembleExamPaperRequest;
import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.service.ExamService;
import com.example.onlineexam.service.ExamSessionService;
import com.example.onlineexam.service.StudentExamService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;
import com.example.onlineexam.dto.StudentExamListItemDto;

/**
 * 试卷接口
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final ExamSessionService examSessionService;
    private final StudentExamService studentExamService;

    public ExamController(ExamService examService, ExamSessionService examSessionService, StudentExamService studentExamService) {
        this.examService = examService;
        this.examSessionService = examSessionService;
        this.studentExamService = studentExamService;
    }

    /**
     * 创建试卷
     */
    @PostMapping
    public ApiResponse create(@RequestBody ExamPaper examPaper) {
        ExamPaper saved = examService.createExamPaper(examPaper);
        return ApiResponse.success("创建试卷成功", saved);
    }

    /**
     * 创建试卷 + 选题组卷（一次性创建 exam_papers + exam_questions）
     */
    @PostMapping("/with-questions")
    public ApiResponse createWithQuestions(@RequestBody CreateExamPaperRequest req, HttpSession session) {
        // ============================
        // 权限/登录校验
        // ============================
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能创建试卷");
        }

        Long userId = (Long) session.getAttribute("userId");
        Long createdBy = req.getCreatedBy() != null ? req.getCreatedBy() : userId;

        try {
            ExamPaper saved = examService.createExamPaperWithQuestions(req, createdBy);
            return ApiResponse.success("创建试卷并组卷成功", saved);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询试卷列表，可按创建教师筛选
     */
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long createdBy) {
        List<ExamPaper> examPapers = examService.listExamPapers(createdBy);
        return ApiResponse.success("查询试卷列表成功", examPapers);
    }

    /**
     * 获取试卷详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        ExamPaper examPaper = examService.getExamPaper(id);
        return ApiResponse.success("获取试卷详情成功", examPaper);
    }

    /**
     * 更新试卷
     */
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody ExamPaper examPaper) {
        ExamPaper updated = examService.updateExamPaper(id, examPaper);
        return ApiResponse.success("更新试卷成功", updated);
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        examService.deleteExamPaper(id);
        return ApiResponse.success("删除试卷成功", null);
    }

    /**
     * 获取学生可参加的考试列表
     */
    @GetMapping("/available")
    public ApiResponse availableExams() {
        List<ExamSession> availableExams = examSessionService.getAvailableExamSessions(LocalDateTime.now());
        return ApiResponse.success("查询可参加考试列表成功", availableExams);
    }

    /**
     * 获取学生已参加的考试列表
     */
    @GetMapping("/my")
    public ApiResponse myExams(@RequestParam Long studentId) {
        List<StudentExam> myExams = studentExamService.getStudentExamsByStudentId(studentId);
        return ApiResponse.success("查询我的考试列表成功", myExams);
    }

    /**
     * 编辑试卷 + 重新选题组卷
     */
    @PutMapping("/{id}/with-questions")
    public ApiResponse updateWithQuestions(@PathVariable Long id, @RequestBody UpdateExamPaperRequest req, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能编辑试卷");
        }
        Long teacherId = (Long) session.getAttribute("userId");

        try {
            ExamPaper saved = examService.updateExamPaperWithQuestions(id, req, teacherId);
            return ApiResponse.success("更新试卷并重新组卷成功", saved);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 自动随机组卷：老师提交“按题型分别设置：题数 + 难度（支持每个难度数量）”的规则
     * 后端按规则随机抽题，并创建试卷 + 试卷题目关联。
     */
    @PostMapping("/auto-assemble")
    public ApiResponse autoAssemble(@RequestBody AutoAssembleExamPaperRequest req, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能自动组卷");
        }

        Long teacherId = (Long) session.getAttribute("userId");
        try {
            ExamPaper saved = examService.autoAssembleExamPaper(req, teacherId);
            return ApiResponse.success("自动组卷成功", saved);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 学生仪表板：我的考试（合并“可参加考试”与“我的考试”）
     *
     * 返回字段：考试场次ID、标题、时长、试卷ID、开始/结束时间、状态(未开始/进行中/已完成)
     */
    @GetMapping("/my-dashboard")
    public ApiResponse myDashboard(@RequestParam Long studentId) {
        List<StudentExamListItemDto> list = studentExamService.getMyExamList(studentId);
        return ApiResponse.success("查询我的考试(仪表板)成功", list);
    }
}