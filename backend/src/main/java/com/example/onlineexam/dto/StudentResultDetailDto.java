package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 学生成绩详情：一份试卷/一次考试的整体详情
 */
@Data
public class StudentResultDetailDto {

    /** student_exams.id */
    private Long studentExamId;

    /** 考试场次ID */
    private Long examSessionId;

    /** 考试标题 */
    private String examTitle;

    /** 得分 */
    private Integer score;

    /** 总分 */
    private Integer totalScore;

    /** 单题明细 */
    private List<ResultDetailItemDto> items;
}

