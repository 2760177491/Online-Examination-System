package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 考试分配仓库
 */
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {

    boolean existsByExamSessionIdAndStudentId(Long examSessionId, Long studentId);

    List<ExamAssignment> findByExamSessionId(Long examSessionId);

    List<ExamAssignment> findByStudentId(Long studentId);

    @Modifying
    @Transactional
    void deleteByExamSessionIdAndStudentId(Long examSessionId, Long studentId);

    // ============================
    // 教师端优化：分配人数统计 + 一键清空
    // ============================
    long countByExamSessionId(Long examSessionId);

    @Modifying
    @Transactional
    void deleteByExamSessionId(Long examSessionId);
}
