package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师端：进入某个学生考试记录后的批改页面数据
 */
@Data
public class TeacherSubjectiveAnswerListDto {

    private Long studentExamId;
    private Long examSessionId;
    private String examTitle;

    private Long studentId;
    private String studentUsername;
    private String studentRealName;

    private LocalDateTime submitTime;

    /** 当前总分（含客观题 + 已批改主观题） */
    private Integer currentTotalScore;

    /** 试卷总分 */
    private Integer totalScore;

    /** 主观题列表 */
    private List<TeacherSubjectiveAnswerItemDto> items;
}

