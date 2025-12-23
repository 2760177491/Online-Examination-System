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

    /**
     * 教师创建考试场次（更推荐使用这个）
     * - 只需要传 examPaperId/name/startTime
     * - endTime/durationMinutes/status 由后端根据试卷时长与当前时间自动计算
     */
    ExamSession createExamSessionByPaper(Long examPaperId, String name, java.time.LocalDateTime startTime, Long createdBy);

    /**
     * 获取教师的所有考试场次（不过滤时间）
     */
    java.util.List<ExamSession> getAllExamSessionsByTeacherId(Long teacherId);
}