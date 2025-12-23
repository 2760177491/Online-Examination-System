package com.example.onlineexam.dto;

import java.util.Map;

/**
 * 自动组卷规则（单个题型维度）
 *
 * 例子：
 * - type = single_choice
 * - counts = {"简单":2, "中等":2, "困难":1}
 */
public class AutoAssembleRuleItem {

    /**
     * 题型：single_choice / multiple_choice / true_false / subjective
     */
    private String type;

    /**
     * 难度 -> 题数
     * key: 简单/中等/困难
     * value: 数量（>=0）
     */
    private Map<String, Integer> counts;

    // ============================
    // 显式 Getter/Setter
    // ============================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Integer> counts) {
        this.counts = counts;
    }
}
