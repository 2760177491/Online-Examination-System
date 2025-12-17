package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 考试场次仓库
 */
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    
    /**
     * 根据创建教师ID查询考试场次列表
     */
    List<ExamSession> findByCreatedBy(Long createdBy);
    
    /**
     * 查询当前可参加的考试（进行中或未开始但即将开始的考试）
     */
    List<ExamSession> findByEndTimeAfterAndStartTimeBefore(LocalDateTime currentTime1, LocalDateTime currentTime2);
    
    /**
     * 查询结束时间在当前时间之后的考试场次
     */
    List<ExamSession> findByEndTimeAfter(LocalDateTime currentTime);
}