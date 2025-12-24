package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.service.ExamSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试场次接口
 */
@RestController
@RequestMapping("/api/exam-sessions")
public class ExamSessionController {
    
    private final ExamSessionService examSessionService;
    
    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }
    
    /**
     * 创建考试场次
     */
    @PostMapping
    public ApiResponse create(@RequestBody ExamSession examSession) {
        ExamSession saved = examSessionService.createExamSession(examSession);
        return ApiResponse.success("创建考试场次成功", saved);
    }
    
    /**
     * 获取考试场次列表
     */
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long createdBy) {
        List<ExamSession> examSessions;
        if (createdBy != null) {
            examSessions = examSessionService.getExamSessionsByTeacherId(createdBy);
        } else {
            examSessions = examSessionService.getAvailableExamSessions(LocalDateTime.now());
        }
        return ApiResponse.success("查询考试场次列表成功", examSessions);
    }
    
    /**
     * 获取考试场次详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        ExamSession examSession = examSessionService.getExamSessionById(id);
        return ApiResponse.success("获取考试场次详情成功", examSession);
    }
    
    /**
     * 更新考试场次
     */
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody ExamSession examSession) {
        ExamSession updated = examSessionService.updateExamSession(id, examSession);
        return ApiResponse.success("更新考试场次成功", updated);
    }
    
    /**
     * 删除考试场次
     */
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        examSessionService.deleteExamSession(id);
        return ApiResponse.success("删除考试场次成功", null);
    }

    /**
     * 教师创建考试场次（推荐使用）
     * 前端只传：试卷ID + 考试名称 + 开始时间
     * 后端根据试卷默认时长自动计算 endTime/durationMinutes 并设置 status
     */
    @PostMapping("/create-by-paper")
    public ApiResponse createByPaper(@RequestBody com.example.onlineexam.dto.CreateExamSessionRequest req) {
        if (req == null) {
            return ApiResponse.error("请求不能为空");
        }
        if (req.getExamPaperId() == null) {
            return ApiResponse.error("examPaperId 不能为空");
        }
        if (req.getStartTime() == null) {
            return ApiResponse.error("startTime 不能为空");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            return ApiResponse.error("考试名称不能为空");
        }
        if (req.getCreatedBy() == null) {
            return ApiResponse.error("createdBy 不能为空（请先登录教师账号）");
        }

        // 当前版本优先实现“从试卷自动带出时长”。
        // 如果你传了 overrideDurationMinutes，则覆盖试卷默认时长。
        // 为了最小改动，这里先在 controller 里做一个简单覆盖：
        if (req.getOverrideDurationMinutes() != null && req.getOverrideDurationMinutes() > 0) {
            // 直接构造一个 ExamSession 走原 create 接口逻辑（保持兼容）
            com.example.onlineexam.entity.ExamSession es = new com.example.onlineexam.entity.ExamSession();
            es.setExamPaperId(req.getExamPaperId());
            es.setName(req.getName().trim());
            es.setStartTime(req.getStartTime());
            es.setDurationMinutes(req.getOverrideDurationMinutes());
            es.setEndTime(req.getStartTime().plusMinutes(req.getOverrideDurationMinutes()));
            es.setCreatedBy(req.getCreatedBy());
            com.example.onlineexam.entity.ExamSession saved = examSessionService.createExamSession(es);
            return ApiResponse.success("创建考试场次成功", saved);
        }

        ExamSession saved = examSessionService.createExamSessionByPaper(
                req.getExamPaperId(),
                req.getName(),
                req.getStartTime(),
                req.getCreatedBy()
        );
        return ApiResponse.success("创建考试场次成功", saved);
    }

    /**
     * 教师端：获取自己所有考试场次（考试管理/成绩分析/监控用，不按时间过滤）
     *
     * 修复说明：
     * - 之前要求必须传 createdBy，前端没有传就会 400（Bad Request）。
     * - 现在改为：优先从 session 取 userId（更真实也更安全），并兼容旧版 createdBy 参数。
     */
    @GetMapping("/teacher")
    public ApiResponse listTeacherAll(@RequestParam(required = false) Long createdBy, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以查看考试场次");
        }

        // 中文注释：优先使用登录态里的教师ID；如果没有(兼容历史问题)，再用请求参数 createdBy
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) {
            teacherId = createdBy;
        }
        if (teacherId == null) {
            return ApiResponse.error("无法获取教师ID，请重新登录后再试");
        }

        List<ExamSession> list = examSessionService.getAllExamSessionsByTeacherId(teacherId);
        return ApiResponse.success("查询教师考试场次成功", list);
    }
}