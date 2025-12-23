-- =========================================================
-- 迁移脚本：为 exam_papers 增加考试时长字段（分钟）
-- 说明：
-- 1) 你的 exam_sessions 已经有 duration_minutes，但试卷(模板)也需要默认时长。
-- 2) 本脚本做“幂等”处理：如果字段已存在不会重复添加。
-- 3) 默认值暂设为 60 分钟，你可按需要调整。
-- =========================================================

-- MySQL: 判断列是否存在（information_schema）
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'exam_papers'
    AND COLUMN_NAME = 'duration_minutes'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE exam_papers ADD COLUMN duration_minutes INT NOT NULL DEFAULT 60 COMMENT "考试时长(分钟)，试卷默认时长";',
  'SELECT "Column duration_minutes already exists on exam_papers" AS msg;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 可选：把现有试卷的时长按你的已存在场次回填（如果场次有时长）
-- 注意：如果同一试卷有多场考试，取最大/最新由你决定。
-- 这里给一个“取最大时长”的示例：
UPDATE exam_papers ep
JOIN (
  SELECT exam_paper_id, MAX(duration_minutes) AS max_duration
  FROM exam_sessions
  GROUP BY exam_paper_id
) s ON s.exam_paper_id = ep.id
SET ep.duration_minutes = s.max_duration
WHERE ep.duration_minutes IS NULL OR ep.duration_minutes = 60;

