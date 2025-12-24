package com.example.onlineexam.dto;

import lombok.Data;

import java.util.List;

/**
 * 教师端：主观题批改请求（支持批量）
 */
@Data
public class TeacherGradeSubjectiveRequest {

    /** student_exams.id */
    private Long studentExamId;

    /** 批改项列表 */
    private List<Item> items;

    @Data
    public static class Item {
        /** 题目ID */
        private Long questionId;
        /** 本题得分（0~满分） */
        private Integer score;
    }
}

