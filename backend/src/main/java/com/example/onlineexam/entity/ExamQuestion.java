package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 试卷与题目关联
 */
@Entity
@Table(name = "exam_questions")
@Data
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 试卷ID */
    @Column(nullable = false)
    private Long examPaperId;

    /** 题目ID */
    @Column(nullable = false)
    private Long questionId;

    /** 题号 */
    @Column(nullable = false)
    private Integer orderIndex;

    /** 此题在该试卷的分值 */
    @Column(nullable = false)
    private Integer score;
}