package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 单题得分情况 DTO
 */
@Data
public class QuestionScoreDto {

    /** 题目ID */
    private Long questionId;

    /** 题目在试卷中的序号（第几题） */
    private Integer orderIndex;

    /** 本题满分 */
    private Integer questionScore;

    /** 学生本题得分 */
    private Integer obtainedScore;

    /** 题目类型（single_choice / multiple_choice / true_false / subjective 等） */
    private String questionType;

    /** 正确答案（字符串形式，选择题通常是 A,B,C...） */
    private String correctAnswer;

    /** 学生答案（字符串形式，与前端传入 / 存库格式一致） */
    private String studentAnswer;
}

