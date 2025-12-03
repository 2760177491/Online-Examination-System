package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 试卷题目关联仓库
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamPaperIdOrderByOrderIndex(Long examPaperId);
}