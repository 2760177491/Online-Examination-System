package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目实体，包含题干、题型、选项与答案
 */
@Entity
@Table(name = "questions")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 题干 */
    @Column(nullable = false, length = 1000)
    private String title;

    /** 题型：single_choice / true_false / subjective */
    @Column(nullable = false, length = 32)
    private String type;

    /**
     * 难度：简单 / 中等 / 困难
     * 注意：
     * - 这里用中文字符串，和你数据库里更直观一致
     * - 为了兼容旧数据与前端未传值的情况，服务端会兜底为“中等”
     */
    @Column(nullable = false, length = 16)
    private String difficulty = "中等";

    /** 选项 JSON（主观题可为空） */
    @Lob
    private String optionsJson;

    /** 正确答案 */
    @Column(nullable = false, length = 255)
    private String correctAnswer;

    /** 分值 */
    @Column(nullable = false)
    private Integer score;

    /** 出题教师 ID */
    @Column(nullable = false)
    private Long createdBy;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}