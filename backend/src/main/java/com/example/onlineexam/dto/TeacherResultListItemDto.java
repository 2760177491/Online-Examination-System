package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端：某场考试的成绩列表项
 */
@Data
public class TeacherResultListItemDto {

    private Long studentExamId;

    private Long studentId;

    private String studentUsername;

    private String studentRealName;

    private Integer score;

    private Integer totalScore;

    private Double scoreRate;

    private LocalDateTime submitTime;

    private String status;
}

