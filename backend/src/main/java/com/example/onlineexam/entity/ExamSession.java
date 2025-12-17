package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试场次实体
 */
@Entity
@Table(name = "exam_sessions")
@Data
public class ExamSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 试卷ID */
    @Column(nullable = false)
    private Long examPaperId;
    
    /** 考试名称 */
    @Column(nullable = false, length = 255)
    private String name;
    
    /** 开始时间 */
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    /** 结束时间 */
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    /** 考试时长（分钟） */
    @Column(nullable = false)
    private Integer durationMinutes;
    
    /** 考试状态：未开始、进行中、已结束 */
    @Column(nullable = false, length = 32)
    private String status;
    
    /** 创建教师ID */
    @Column(nullable = false)
    private Long createdBy;
    
    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}