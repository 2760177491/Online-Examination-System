package com.example.onlineexam.repository;

import com.example.onlineexam.entity.ExamMonitorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamMonitorLogRepository extends JpaRepository<ExamMonitorLog, Long> {

    Optional<ExamMonitorLog> findByExamSessionIdAndStudentId(Long examSessionId, Long studentId);

    List<ExamMonitorLog> findByExamSessionId(Long examSessionId);
}

