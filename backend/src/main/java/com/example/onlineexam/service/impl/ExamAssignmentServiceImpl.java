package com.example.onlineexam.service.impl;

import com.example.onlineexam.entity.ExamAssignment;
import com.example.onlineexam.repository.ExamAssignmentRepository;
import com.example.onlineexam.service.ExamAssignmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 考试分配服务实现
 */
@Service
public class ExamAssignmentServiceImpl implements ExamAssignmentService {

    private final ExamAssignmentRepository examAssignmentRepository;

    public ExamAssignmentServiceImpl(ExamAssignmentRepository examAssignmentRepository) {
        this.examAssignmentRepository = examAssignmentRepository;
    }

    @Override
    public List<ExamAssignment> assignStudents(Long examSessionId, List<Long> studentIds, Long teacherId) {
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId 不能为空");
        }
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExamAssignment> saved = new ArrayList<>();
        for (Long sid : studentIds) {
            if (sid == null) continue;
            if (examAssignmentRepository.existsByExamSessionIdAndStudentId(examSessionId, sid)) {
                continue; // 已存在则跳过（幂等）
            }
            ExamAssignment ea = new ExamAssignment();
            ea.setExamSessionId(examSessionId);
            ea.setStudentId(sid);
            ea.setCreatedBy(teacherId);
            ea.setCreatedAt(LocalDateTime.now());
            saved.add(examAssignmentRepository.save(ea));
        }
        return saved;
    }

    @Override
    public void revoke(Long examSessionId, Long studentId) {
        if (examSessionId == null || studentId == null) {
            throw new IllegalArgumentException("examSessionId/studentId 不能为空");
        }
        examAssignmentRepository.deleteByExamSessionIdAndStudentId(examSessionId, studentId);
    }

    @Override
    public List<ExamAssignment> listByExamSession(Long examSessionId) {
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId 不能为空");
        }
        return examAssignmentRepository.findByExamSessionId(examSessionId);
    }

    @Override
    public List<ExamAssignment> listByStudent(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        return examAssignmentRepository.findByStudentId(studentId);
    }

    @Override
    public boolean isAssigned(Long examSessionId, Long studentId) {
        if (examSessionId == null || studentId == null) {
            return false;
        }
        return examAssignmentRepository.existsByExamSessionIdAndStudentId(examSessionId, studentId);
    }

    @Override
    public long countByExamSession(Long examSessionId) {
        if (examSessionId == null) {
            return 0;
        }
        return examAssignmentRepository.countByExamSessionId(examSessionId);
    }

    @Override
    public void clearByExamSession(Long examSessionId) {
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId 不能为空");
        }
        examAssignmentRepository.deleteByExamSessionId(examSessionId);
    }
}
