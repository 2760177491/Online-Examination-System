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

    /**
     * 题干关键字模糊查询（忽略大小写）
     */
    List<Question> findByTitleContainingIgnoreCase(String keyword);

    /**
     * 组合筛选：createdBy + type + 题干关键字（忽略大小写，包含匹配）
     */
    List<Question> findByCreatedByAndTypeAndTitleContainingIgnoreCase(Long createdBy, String type, String keyword);

    /**
     * 组合筛选：createdBy + 题干关键字
     */
    List<Question> findByCreatedByAndTitleContainingIgnoreCase(Long createdBy, String keyword);

    /**
     * 组合筛选：type + 题干关键字
     */
    List<Question> findByTypeAndTitleContainingIgnoreCase(String type, String keyword);

    /**
     * 组合筛选：createdBy + type
     */
    List<Question> findByCreatedByAndType(Long createdBy, String type);

    /**
     * 只按难度筛选
     */
    List<Question> findByDifficulty(String difficulty);

    /**
     * difficulty + keyword
     */
    List<Question> findByDifficultyAndTitleContainingIgnoreCase(String difficulty, String keyword);

    /**
     * type + difficulty
     */
    List<Question> findByTypeAndDifficulty(String type, String difficulty);

    /**
     * createdBy + difficulty
     */
    List<Question> findByCreatedByAndDifficulty(Long createdBy, String difficulty);

    /**
     * createdBy + type + difficulty
     */
    List<Question> findByCreatedByAndTypeAndDifficulty(Long createdBy, String type, String difficulty);

    /**
     * createdBy + difficulty + keyword
     */
    List<Question> findByCreatedByAndDifficultyAndTitleContainingIgnoreCase(Long createdBy, String difficulty, String keyword);

    /**
     * type + difficulty + keyword
     */
    List<Question> findByTypeAndDifficultyAndTitleContainingIgnoreCase(String type, String difficulty, String keyword);

    /**
     * createdBy + type + difficulty + keyword
     */
    List<Question> findByCreatedByAndTypeAndDifficultyAndTitleContainingIgnoreCase(Long createdBy, String type, String difficulty, String keyword);
}