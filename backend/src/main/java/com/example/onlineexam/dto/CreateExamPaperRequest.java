package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建试卷 + 选题组卷请求
 *
 * 说明：
 * - 前端在“创建试卷页面”一次性提交试卷基本信息和题目ID列表。
 * - 后端会：
 *   1) 创建 exam_papers
 *   2) 创建 exam_questions（按顺序写入 order_index，并使用题目分值作为该题 score）
 */
@Data
public class CreateExamPaperRequest {

    /** 试卷名称 */
    private String name;

    /** 试卷说明 */
    private String description;

    /**
     * 试卷默认考试时长（分钟）
     * - 试卷是“模板”，时长属于模板默认属性
     */
    private Integer durationMinutes;

    /** 总分（前端可传；后端也会按题目分值重新计算并覆盖，避免不一致） */
    private Integer totalScore;

    /** 创建教师ID（前端不传也可以，后端会从 HttpSession 中兜底） */
    private Long createdBy;

    /** 选中的题目ID列表（按前端顺序） */
    private List<Long> questionIds;
}
