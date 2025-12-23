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

    /**
     * 考试时长（分钟）
     * 说明：试卷是“模板”，时长属于模板的默认属性；发布考试(ExamSession)时可继承，也允许覆盖。
     * 注意：需要先执行数据库迁移脚本为 exam_papers 添加 duration_minutes 列。
     */
    private Integer durationMinutes;

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