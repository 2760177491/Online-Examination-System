package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生考试记录实体
 */
@Entity
@Table(name = "student_exams")
@Data
public class StudentExam {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 考试场次ID */
    @Column(nullable = false)
    private Long examSessionId;
    
    /** 学生ID */
    @Column(nullable = false)
    private Long studentId;
    
    /** 实际开始时间 */
    @Column(nullable = false)
    private LocalDateTime actualStartTime;
    
    /** 提交时间 */
    private LocalDateTime submitTime;
    
    /** 总分 */
    private Integer totalScore;
    
    /** 考试状态：进行中、已提交、已批改 */
    @Column(nullable = false, length = 32)
    private String status;
    
    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}