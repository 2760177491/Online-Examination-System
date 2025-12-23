package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 教师分配考试给学生：批量分配请求
 */
@Data
public class AssignStudentsRequest {

    /** 考试场次ID */
    private Long examSessionId;

    /** 要分配的学生ID列表 */
    private List<Long> studentIds;
}

