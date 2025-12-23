package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 试卷题目关联仓库
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamPaperIdOrderByOrderIndex(Long examPaperId);

    /**
     * 删除某张试卷下的所有关联题目（用于编辑试卷时重新组卷）
     */
    void deleteByExamPaperId(Long examPaperId);
}