package com.example.onlineexam.repository;

import com.example.onlineexam.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 学生答案仓库
 */
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    
    /**
     * 根据学生考试ID查询所有答案
     */
    List<StudentAnswer> findByStudentExamId(Long studentExamId);
    
    /**
     * 根据学生考试ID和题目ID查询特定答案
     */
    StudentAnswer findByStudentExamIdAndQuestionId(Long studentExamId, Long questionId);
}