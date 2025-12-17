package com.example.onlineexam.service;

import com.example.onlineexam.entity.ExamSession;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 考试场次服务
 */
public interface ExamSessionService {
    
    /**
     * 创建考试场次
     */
    ExamSession createExamSession(ExamSession examSession);
    
    /**
     * 获取考试场次详情
     */
    ExamSession getExamSessionById(Long id);
    
    /**
     * 更新考试场次
     */
    ExamSession updateExamSession(Long id, ExamSession examSession);
    
    /**
     * 删除考试场次
     */
    void deleteExamSession(Long id);
    
    /**
     * 根据教师ID获取考试场次列表
     */
    List<ExamSession> getExamSessionsByTeacherId(Long teacherId);
    
    /**
     * 获取学生可参加的考试列表
     */
    List<ExamSession> getAvailableExamSessions(LocalDateTime currentTime);
    
    /**
     * 更新考试场次状态
     */
    void updateExamSessionStatus(Long id, String status);
}