package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 教师端：批改单个主观题的展示 DTO
 */
@Data
public class TeacherSubjectiveAnswerItemDto {

    /** student_answers.id */
    private Long studentAnswerId;

    /** student_exams.id */
    private Long studentExamId;

    /** 题目ID */
    private Long questionId;

    /** 题目顺序（orderIndex） */
    private Integer orderIndex;

    /** 题干/标题 */
    private String title;

    /** 题目类型（一般为 subjective） */
    private String type;

    /** 分值（本题满分） */
    private Integer fullScore;

    /** 参考答案（题库里的 correctAnswer，可为空） */
    private String referenceAnswer;

    /** 学生作答内容 */
    private String studentAnswer;

    /** 得分（未批改时可能为 null） */
    private Integer score;

    /** 批改状态：未批改 / 已批改 */
    private String gradingStatus;
}

