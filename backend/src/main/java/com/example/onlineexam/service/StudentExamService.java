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
}