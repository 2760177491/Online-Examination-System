package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 自动判分结果 DTO
 */
@Data
public class GradingResultDto {

    /** 学生考试ID */
    private Long studentExamId;

    /** 本次考试总得分 */
    private Integer totalScore;

    /** 题目总数 */
    private Integer totalQuestions;

    /** 每题得分情况列表 */
    private List<QuestionScoreDto> questionScores;
}

