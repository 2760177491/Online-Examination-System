package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.service.ExamSessionService;
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
}