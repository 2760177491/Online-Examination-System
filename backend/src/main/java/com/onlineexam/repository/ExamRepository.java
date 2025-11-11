package com.onlineexam.repository;

import com.onlineexam.entity.Exam;
import com.onlineexam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCreatedBy(User createdBy);
    List<Exam> findByStatus(Exam.ExamStatus status);

    @Query("SELECT e FROM Exam e WHERE e.startTime <= :now AND e.endTime >= :now")
    List<Exam> findActiveExams(LocalDateTime now);
}