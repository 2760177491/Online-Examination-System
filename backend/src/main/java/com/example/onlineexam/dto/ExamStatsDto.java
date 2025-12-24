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

    /**
     * 成绩分布（区间 -> 人数）
     * 例如：{"0-60": 2, "60-70": 5, "70-80": 3, "80-90": 1, "90-100": 0}
     */
    private java.util.Map<String, Integer> scoreDistribution;

    public long getTotalParticipants() {
        return totalParticipants;
    }

    public void setTotalParticipants(long totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public long getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(long submittedCount) {
        this.submittedCount = submittedCount;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public java.util.Map<String, Integer> getScoreDistribution() {
        return scoreDistribution;
    }

    public void setScoreDistribution(java.util.Map<String, Integer> scoreDistribution) {
        this.scoreDistribution = scoreDistribution;
    }
}
