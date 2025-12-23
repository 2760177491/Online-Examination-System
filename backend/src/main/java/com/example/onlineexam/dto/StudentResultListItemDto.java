package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生端“我的成绩”列表条目 DTO
 *
 * 说明：
 * - 你的 student_exams 表里 totalScore 表示“实际得分”(提交后会写入)
 * - totalScore(试卷总分) 需要从 exam_papers.total_score 读取
 */
@Data
public class StudentResultListItemDto {

    /** 对应 student_exams.id */
    private Long studentExamId;

    /** 考试场次ID */
    private Long examSessionId;

    /** 考试标题（exam_sessions.name） */
    private String examTitle;

    /** 得分（student_exams.total_score，可能为 null，则前端显示 0） */
    private Integer score;

    /** 总分（exam_papers.total_score） */
    private Integer totalScore;

    /** 提交时间 */
    private LocalDateTime submitTime;
}

