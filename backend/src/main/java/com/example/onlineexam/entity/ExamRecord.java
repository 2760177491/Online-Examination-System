package com.onlineexam.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_records")
@Data
public class ExamRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime startTime;
    private LocalDateTime submitTime;

    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    private RecordStatus status = RecordStatus.IN_PROGRESS;

    public enum RecordStatus {
        IN_PROGRESS, SUBMITTED, GRADED
    }
}