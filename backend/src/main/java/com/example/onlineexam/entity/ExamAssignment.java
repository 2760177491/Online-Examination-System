package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试分配实体：用于控制“某场考试(ExamSession)分配给哪些学生”。
 *
 * 说明：
 * - 这是第9周“分配给学生”的补强点。
 * - 一条记录代表：studentId 被分配了 examSessionId 这场考试。
 */
@Entity
@Table(name = "exam_assignments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exam_assignment", columnNames = {"exam_session_id", "student_id"})
        })
@Data
public class ExamAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 考试场次ID（exam_sessions.id） */
    @Column(name = "exam_session_id", nullable = false)
    private Long examSessionId;

    /** 学生ID（users.id） */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 分配该考试的教师ID（审计用，可选但很有用） */
    @Column(name = "created_by")
    private Long createdBy;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

