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
    
    @Autowired
    private com.example.onlineexam.repository.ExamPaperRepository examPaperRepository;

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
        // 更严格：只返回“当前时刻在时间窗内的考试”，避免未分配/未来考试被误当成可参加
        // 学生真正可参加列表请使用 ExamAssignment + StudentExamService.getMyExamList
        return examSessionRepository.findByEndTimeAfterAndStartTimeBefore(currentTime, currentTime);
    }
    
    @Override
    public void updateExamSessionStatus(Long id, String status) {
        ExamSession examSession = getExamSessionById(id);
        examSession.setStatus(status);
        examSessionRepository.save(examSession);
    }

    @Override
    public ExamSession createExamSessionByPaper(Long examPaperId, String name, LocalDateTime startTime, Long createdBy) {
        if (examPaperId == null) {
            throw new IllegalArgumentException("examPaperId 不能为空");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy 不能为空（教师未登录）");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("考试名称不能为空");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime 不能为空");
        }

        // 1) 读取试卷模板，拿到默认考试时长
        com.example.onlineexam.entity.ExamPaper paper = examPaperRepository.findById(examPaperId)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在：" + examPaperId));

        // 2) 如果试卷未设置时长，给一个兜底（60分钟）
        int duration = (paper.getDurationMinutes() == null || paper.getDurationMinutes() <= 0)
                ? 60
                : paper.getDurationMinutes();

        // 3) 自动计算结束时间
        LocalDateTime endTime = startTime.plusMinutes(duration);

        ExamSession session = new ExamSession();
        session.setExamPaperId(examPaperId);
        session.setName(name.trim());
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setDurationMinutes(duration);
        session.setCreatedBy(createdBy);
        session.setCreatedAt(LocalDateTime.now());

        // 4) 根据当前时间计算状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            session.setStatus("未开始");
        } else if (now.isBefore(session.getEndTime())) {
            session.setStatus("进行中");
        } else {
            session.setStatus("已结束");
        }

        return examSessionRepository.save(session);
    }

    @Override
    public List<ExamSession> getAllExamSessionsByTeacherId(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId 不能为空");
        }
        return examSessionRepository.findByCreatedBy(teacherId);
    }
}