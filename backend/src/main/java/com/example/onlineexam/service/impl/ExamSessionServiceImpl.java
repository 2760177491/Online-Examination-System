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
        List<ExamSession> list = examSessionRepository.findByCreatedBy(teacherId);
        // 中文注释：每次查询时根据当前时间刷新状态，避免“昨天创建的场次今天还显示进行中”的问题
        refreshAndPersistStatusIfNeeded(list);
        return list;
    }
    
    @Override
    public List<ExamSession> getAvailableExamSessions(LocalDateTime currentTime) {
        List<ExamSession> list = examSessionRepository.findByEndTimeAfterAndStartTimeBefore(currentTime, currentTime);
        // 中文注释：可参加列表同样刷新一下状态（主要是兜底数据异常/手工改时间）
        refreshAndPersistStatusIfNeeded(list);
        return list;
    }

    @Override
    public List<ExamSession> getAllExamSessionsByTeacherId(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId 不能为空");
        }
        List<ExamSession> list = examSessionRepository.findByCreatedBy(teacherId);
        refreshAndPersistStatusIfNeeded(list);
        return list;
    }

    /**
     * 中文注释：根据 startTime/endTime 动态计算正确状态。
     * - 未开始：now < startTime
     * - 进行中：startTime <= now < endTime
     * - 已结束：now >= endTime
     *
     * 并在状态发生变化时写回数据库（保证列表页刷新后状态正确）。
     */
    private void refreshAndPersistStatusIfNeeded(List<ExamSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        for (ExamSession s : sessions) {
            if (s == null || s.getStartTime() == null || s.getEndTime() == null) continue;
            String newStatus;
            if (now.isBefore(s.getStartTime())) {
                newStatus = "未开始";
            } else if (now.isBefore(s.getEndTime())) {
                newStatus = "进行中";
            } else {
                newStatus = "已结束";
            }

            if (s.getStatus() == null || !s.getStatus().equals(newStatus)) {
                s.setStatus(newStatus);
                changed = true;
            }
        }

        if (changed) {
            // 中文注释：批量保存（JPA 会按主键更新）
            examSessionRepository.saveAll(sessions);
        }
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
}

