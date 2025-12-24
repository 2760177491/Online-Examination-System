package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端：待批改列表条目
 * 一条记录对应一次学生考试（student_exams）中“仍存在未批改主观题”的情况。
 */
@Data
public class TeacherSubjectivePendingItemDto {

    /** student_exams.id */
    private Long studentExamId;

    /** exam_sessions.id */
    private Long examSessionId;

    /** 考试标题（场次名） */
    private String examTitle;

    /** 学生信息 */
    private Long studentId;
    private String studentUsername;
    private String studentRealName;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 待批改主观题数量 */
    private Integer pendingCount;

    /** 本次考试当前总分（含客观题自动判分 + 已批改主观题） */
    private Integer currentTotalScore;
}

