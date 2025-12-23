package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 编辑试卷（基本信息 + 重新选题组卷）
 */
@Data
public class UpdateExamPaperRequest {

    /** 试卷名称 */
    private String name;

    /** 说明 */
    private String description;

    /** 试卷默认考试时长（分钟） */
    private Integer durationMinutes;

    /** 重新选题后的题目ID列表（按前端顺序） */
    private List<Long> questionIds;
}
