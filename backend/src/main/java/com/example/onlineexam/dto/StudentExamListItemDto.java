package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生端“我的考试”列表条目 DTO
 *
 * 设计目标：
 * - 合并“可参加考试(ExamSession)”与“我的考试(StudentExam)”两种数据视角
 * - 前端只需要展示：考试场次ID、标题、时长、试卷ID、开始/结束时间、状态
 * - status 统一为：未开始 / 进行中 / 已完成
 */
@Data
public class StudentExamListItemDto {

    /** 考试场次ID（exam_sessions.id） */
    private Long examSessionId;

    /** 考试标题（exam_sessions.name） */
    private String examTitle;

    /** 考试时长（分钟）（exam_sessions.duration_minutes） */
    private Integer durationMinutes;

    /** 试卷ID（exam_sessions.exam_paper_id） */
    private Long examPaperId;

    /** 开始时间（exam_sessions.start_time） */
    private LocalDateTime startTime;

    /** 结束时间（exam_sessions.end_time） */
    private LocalDateTime endTime;

    /**
     * 统一状态：未开始 / 进行中 / 已完成
     * 说明：
     * - 只要学生在 student_exams 中对该 examSession 有记录并且 status 为“已提交/已批改”，就判定为“已完成”
     * - 否则按照 exam_sessions.status 映射：未开始/进行中/已结束 -> 未开始/进行中/已完成
     */
    private String status;
}

