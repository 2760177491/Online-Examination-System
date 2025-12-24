-- 第11周：考试监控（简化版）
-- 用于记录学生心跳与切屏次数

CREATE TABLE IF NOT EXISTS exam_monitor_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  last_heartbeat_time DATETIME NULL,
  switch_count INT NOT NULL DEFAULT 0,
  last_event VARCHAR(32) NULL,
  UNIQUE KEY uk_exam_student (exam_session_id, student_id)
);

