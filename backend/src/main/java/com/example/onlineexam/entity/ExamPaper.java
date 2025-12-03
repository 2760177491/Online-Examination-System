package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷实体，记录基本信息
 */
@Entity
@Table(name = "exam_papers")
@Data
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 试卷名称 */
    @Column(nullable = false, length = 255)
    private String name;

    /** 说明 */
    @Column(length = 1000)
    private String description;

    /** 总分 */
    @Column(nullable = false)
    private Integer totalScore;

    /** 创建教师ID */
    @Column(nullable = false)
    private Long createdBy;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}