-- =========================================================
-- 新增：考试分配表 exam_assignments
-- 作用：实现“教师把某场考试(ExamSession)分配给指定学生”，学生端只看到被分配的考试。
--
-- 说明：
-- 1) 你之前是“end_time > now 的考试都对全体学生可见”，这里改为“仅分配可见”。
-- 2) 该脚本为幂等：如果表已存在不会重复创建。
-- =========================================================

CREATE TABLE IF NOT EXISTS exam_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exam_assignment (exam_session_id, student_id)
);

-- 可选：加索引提升查询性能
CREATE INDEX idx_exam_assignment_student ON exam_assignments(student_id);
CREATE INDEX idx_exam_assignment_session ON exam_assignments(exam_session_id);

