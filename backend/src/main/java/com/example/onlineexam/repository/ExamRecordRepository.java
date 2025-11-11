package com.onlineexam.repository;

import com.onlineexam.entity.Exam;
import com.onlineexam.entity.ExamRecord;
import com.onlineexam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {
    Optional<ExamRecord> findByExamAndStudent(Exam exam, User student);
    List<ExamRecord> findByStudent(User student);
    List<ExamRecord> findByExam(Exam exam);
    boolean existsByExamAndStudent(Exam exam, User student);
}