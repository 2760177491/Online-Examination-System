package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 试卷仓库
 */
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {

    /**
     * 根据创建教师ID查询试卷列表
     */
    List<ExamPaper> findByCreatedBy(Long createdBy);
}