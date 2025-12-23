package com.example.onlineexam.service;

import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.entity.StudentAnswer;

import java.util.List;

/**
 * 学生考试服务
 */
public interface StudentExamService {
    
    /**
     * 学生开始考试
     */
    StudentExam startExam(Long studentId, Long examSessionId);
    
    /**
     * 学生提交考试
     */
    StudentExam submitExam(Long studentExamId, List<StudentAnswer> answers);
    
    /**
     * 获取学生考试详情
     */
    StudentExam getStudentExamById(Long id);
    
    /**
     * 根据学生ID获取考试记录列表
     */
    List<StudentExam> getStudentExamsByStudentId(Long studentId);
    
    /**
     * 根据考试场次ID获取所有学生的考试记录
     */
    List<StudentExam> getStudentExamsByExamSessionId(Long examSessionId);
    
    /**
     * 保存学生答案
     */
    StudentAnswer saveStudentAnswer(StudentAnswer answer);
    
    /**
     * 获取学生的所有答案
     */
    List<StudentAnswer> getStudentAnswersByStudentExamId(Long studentExamId);

    /**
     * 学生端：我的考试（合并可参加考试场次 + 学生已参加记录）
     * @param studentId 学生ID
     */
    java.util.List<com.example.onlineexam.dto.StudentExamListItemDto> getMyExamList(Long studentId);

    /**
     * 学生端：我的成绩列表（用于学生仪表板“我的成绩”）
     * @param studentId 学生ID
     */
    java.util.List<com.example.onlineexam.dto.StudentResultListItemDto> getMyResultList(Long studentId);

    /**
     * 学生成绩详情：按 studentExamId 返回每题明细（题干/题型/正确答案/学生答案/得分/批改状态）
     */
    com.example.onlineexam.dto.StudentResultDetailDto getResultDetail(Long studentExamId);

    /**
     * 保存草稿（答题进度保存）
     * - 仅允许 studentExam.status == "进行中"
     * - 按题目维度 upsert（存在则更新，不存在则插入）
     */
    StudentExam saveDraft(Long studentExamId, java.util.List<com.example.onlineexam.entity.StudentAnswer> answers);
}