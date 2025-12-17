package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 考试页面用的试卷题目 DTO
 *
 * 将试卷与题库中的题目信息组合在一起，
 * 方便前端一次性拿到题号、分值、题干、题型和选项等信息。
 */
@Data
public class ExamQuestionDto {

    /** 本题在试卷中的唯一 ID（来自 exam_questions.id） */
    private Long id;

    /** 题号（来自 exam_questions.orderIndex） */
    private Integer orderIndex;

    /** 本题在该试卷中的分值（来自 exam_questions.score） */
    private Integer score;

    /** 原始题目 ID（来自 questions.id），用于后续统计或回显 */
    private Long questionId;

    /** 题干（来自 questions.title） */
    private String content;

    /** 题型：统一使用前端易理解的标识，如 single_choice / true_false / subjective */
    private String type;

    /** 选项 JSON 字符串（来自 questions.optionsJson，单选/判断题使用） */
    private String optionsJson;
}

