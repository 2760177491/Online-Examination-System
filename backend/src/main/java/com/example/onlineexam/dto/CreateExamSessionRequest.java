package com.example.onlineexam.dto;

import java.time.LocalDateTime;

/**
 * 教师创建考试场次请求 DTO
 *
 * 设计目标：让教师创建“考试(ExamSession)”时只关心：
 * - 选哪个试卷模板(examPaperId)
 * - 考试名称(name)
 * - 开始时间(startTime)
 *
 * 结束时间(endTime)与时长(durationMinutes)由后端根据"试卷默认时长"自动计算，避免试卷与考试脱节。
 */
public class CreateExamSessionRequest {

    /** 关联的试卷ID */
    private Long examPaperId;

    /** 考试名称 */
    private String name;

    /** 考试开始时间 */
    private LocalDateTime startTime;

    /** 创建教师ID（前端从登录信息传入） */
    private Long createdBy;

    /**
     * （可选）是否强制覆盖试卷默认时长
     * - null 表示不覆盖
     */
    private Integer overrideDurationMinutes;

    public Long getExamPaperId() {
        return examPaperId;
    }

    public void setExamPaperId(Long examPaperId) {
        this.examPaperId = examPaperId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getOverrideDurationMinutes() {
        return overrideDurationMinutes;
    }

    public void setOverrideDurationMinutes(Integer overrideDurationMinutes) {
        this.overrideDurationMinutes = overrideDurationMinutes;
    }
}
