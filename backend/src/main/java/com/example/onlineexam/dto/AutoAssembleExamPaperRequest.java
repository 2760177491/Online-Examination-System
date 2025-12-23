package com.example.onlineexam.dto;

import java.util.List;

/**
 * 教师端“自动随机组卷”请求
 *
 * 业务含义：
 * - 老师提交：试卷基本信息 + 组卷规则（按题型分别设置：题数 + 难度数量）
 * - 系统根据规则从题库随机抽题，生成最终 questionIds 顺序列表
 */
public class AutoAssembleExamPaperRequest {

    /** 试卷名称 */
    private String name;

    /** 试卷说明 */
    private String description;

    /** 创建教师ID（可不传，后端会从 session 兜底） */
    private Long createdBy;

    /** 是否只从“我的题库”抽题：true=仅抽当前老师出的题；false/null=全库 */
    private Boolean onlyMine;

    /** 组卷规则列表 */
    private List<AutoAssembleRuleItem> rules;

    /** 是否打乱最终题目顺序（默认 false：按规则顺序拼接，便于老师预期） */
    private Boolean shuffleOrder;

    /** 试卷默认考试时长（分钟），不传则后端默认 60 */
    private Integer durationMinutes;

    // ============================
    // 显式 Getter/Setter（避免 Lombok 未启用导致 IDE/编译报 getXxx 找不到）
    // ============================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getOnlyMine() {
        return onlyMine;
    }

    public void setOnlyMine(Boolean onlyMine) {
        this.onlyMine = onlyMine;
    }

    public List<AutoAssembleRuleItem> getRules() {
        return rules;
    }

    public void setRules(List<AutoAssembleRuleItem> rules) {
        this.rules = rules;
    }

    public Boolean getShuffleOrder() {
        return shuffleOrder;
    }

    public void setShuffleOrder(Boolean shuffleOrder) {
        this.shuffleOrder = shuffleOrder;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
