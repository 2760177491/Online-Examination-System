package com.example.onlineexam.service.impl;

import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.repository.ExamSessionRepository;
import com.example.onlineexam.service.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 考试场次服务实现类
 */
@Service
public class ExamSessionServiceImpl implements ExamSessionService {
    
    @Autowired
    private ExamSessionRepository examSessionRepository;
    
    @Override
    public ExamSession createExamSession(ExamSession examSession) {
        // 设置创建时间
        examSession.setCreatedAt(LocalDateTime.now());
        // 根据时间设置初始状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(examSession.getStartTime())) {
            examSession.setStatus("未开始");
        } else if (now.isBefore(examSession.getEndTime())) {
            examSession.setStatus("进行中");
        } else {
            examSession.setStatus("已结束");
        }
        return examSessionRepository.save(examSession);
    }
    
    @Override
    public ExamSession getExamSessionById(Long id) {
        return examSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
    }
    
    @Override
    public ExamSession updateExamSession(Long id, ExamSession examSession) {
        ExamSession existingSession = getExamSessionById(id);
        existingSession.setName(examSession.getName());
        existingSession.setExamPaperId(examSession.getExamPaperId());
        existingSession.setStartTime(examSession.getStartTime());
        existingSession.setEndTime(examSession.getEndTime());
        existingSession.setDurationMinutes(examSession.getDurationMinutes());
        
        // 更新状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(existingSession.getStartTime())) {
            existingSession.setStatus("未开始");
        } else if (now.isBefore(existingSession.getEndTime())) {
            existingSession.setStatus("进行中");
        } else {
            existingSession.setStatus("已结束");
        }
        
        return examSessionRepository.save(existingSession);
    }
    
    @Override
    public void deleteExamSession(Long id) {
        examSessionRepository.deleteById(id);
    }
    
    @Override
    public List<ExamSession> getExamSessionsByTeacherId(Long teacherId) {
        return examSessionRepository.findByCreatedBy(teacherId);
    }
    
    @Override
    public List<ExamSession> getAvailableExamSessions(LocalDateTime currentTime) {
        // 查询当前时间可参加的考试（进行中或未开始但即将开始的考试）
        return examSessionRepository.findByEndTimeAfter(currentTime);
    }
    
    @Override
    public void updateExamSessionStatus(Long id, String status) {
        ExamSession examSession = getExamSessionById(id);
        examSession.setStatus(status);
        examSessionRepository.save(examSession);
    }
}