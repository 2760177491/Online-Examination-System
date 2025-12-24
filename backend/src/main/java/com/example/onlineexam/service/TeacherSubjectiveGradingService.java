package com.example.onlineexam.service;

import com.example.onlineexam.dto.TeacherGradeSubjectiveRequest;
import com.example.onlineexam.dto.TeacherSubjectiveAnswerListDto;
import com.example.onlineexam.dto.TeacherSubjectivePendingItemDto;

import java.util.List;

/**
 * 教师端：主观题批改服务
 */
public interface TeacherSubjectiveGradingService {

    /**
     * 待批改列表：某教师某场考试中，仍存在未批改主观题的学生考试记录
     */
    List<TeacherSubjectivePendingItemDto> listPending(Long examSessionId, Long teacherId);

    /**
     * 打开某个学生考试的批改页面：返回该次考试的主观题明细
     */
    TeacherSubjectiveAnswerListDto getSubjectiveAnswers(Long studentExamId, Long teacherId);

    /**
     * 批量打分：对主观题写入分数并更新状态，同时重算 StudentExam 总分与状态
     */
    TeacherSubjectiveAnswerListDto grade(TeacherGradeSubjectiveRequest req, Long teacherId);
}

