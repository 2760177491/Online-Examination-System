package com.example.onlineexam.repository;

import com.example.onlineexam.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 题库仓库
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 根据创建教师ID查询题目列表
     */
    List<Question> findByCreatedBy(Long createdBy);

    /**
     * 根据题型查询题目列表
     */
    List<Question> findByType(String type);
}