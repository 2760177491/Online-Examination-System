package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 学生答案实体
 */
@Entity
@Table(name = "student_answers")
@Data
public class StudentAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 学生考试ID */
    @Column(nullable = false)
    private Long studentExamId;
    
    /** 题目ID */
    @Column(nullable = false)
    private Long questionId;
    
    /** 学生答案内容 */
    @Column(nullable = false, length = 1000)
    private String answerContent;
    
    /** 得分 */
    private Integer score;
    
    /** 批改状态：未批改、已批改 */
    @Column(nullable = false, length = 32)
    private String gradingStatus;
}