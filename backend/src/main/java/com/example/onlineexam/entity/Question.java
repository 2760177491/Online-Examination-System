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