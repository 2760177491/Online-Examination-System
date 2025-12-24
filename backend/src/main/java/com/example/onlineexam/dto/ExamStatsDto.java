package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 教师端：某场考试的统计信息
 */
@Data
public class ExamStatsDto {

    /** 参考人数（已开始/已提交的 StudentExam 数量） */
    private long totalParticipants;

    /** 已提交人数 */
    private long submittedCount;

    /** 平均分 */
    private double avgScore;

    /** 最高分 */
    private int maxScore;

    /** 最低分 */
    private int minScore;

    /** 及格率（0~1） */
    private double passRate;
}

