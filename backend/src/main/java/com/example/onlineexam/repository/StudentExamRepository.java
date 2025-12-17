package com.example.onlineexam.repository;

import com.example.onlineexam.entity.StudentExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 学生考试记录仓库
 */
public interface StudentExamRepository extends JpaRepository<StudentExam, Long> {
    
    /**
     * 根据学生ID查询考试记录列表
     */
    List<StudentExam> findByStudentId(Long studentId);
    
    /**
     * 根据学生ID和考试场次ID查询考试记录
     */
    StudentExam findByStudentIdAndExamSessionId(Long studentId, Long examSessionId);
    
    /**
     * 根据考试场次ID查询所有学生的考试记录
     */
    List<StudentExam> findByExamSessionId(Long examSessionId);
}