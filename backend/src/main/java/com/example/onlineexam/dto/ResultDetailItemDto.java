package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 学生成绩详情：单题明细
 */
@Data
public class ResultDetailItemDto {

    /** 题目ID */
    private Long questionId;

    /** 题干 */
    private String title;

    /** 题型：single_choice / multiple_choice / true_false / subjective */
    private String type;

    /** 题型中文名：单选题/多选题/判断题/简答题 */
    private String typeLabel;

    /** 题目分值（题库里配置的分值） */
    private Integer questionScore;

    /** 学生答案（原样展示） */
    private String studentAnswer;

    /** 正确答案（客观题可展示；主观题可为空或展示参考答案） */
    private String correctAnswer;

    /** 得分（未批改时可能为 null/0） */
    private Integer score;

    /** 批改状态：未批改 / 已批改 */
    private String gradingStatus;

    /**
     * 是否答对（客观题可准确判断；主观题通常为 null 或 false）
     * - 用于前端红绿样式：对 => 正确答案/我的答案都绿；错 => 正确答案绿、我的答案红
     */
    private Boolean correct;

    /**
     * 选项文本（仅用于客观题辅助对照，可为空）
     * 例如：{"A":"xxx","B":"yyy"}
     */
    private String optionsJson;
}
