-- =========================================================
-- 迁移脚本：为题库 questions 增加 difficulty(难度) 字段（幂等版，可重复执行）
-- 难度取值：简单 / 中等 / 困难
--
-- 典型场景：你已经执行过一次或部分执行过一次，再执行会遇到：
--   1060 - Duplicate column name 'difficulty'
-- 所以这里先判断列是否存在。
-- =========================================================

-- 0) 设置当前数据库名（优先使用当前连接的库）
SET @db := DATABASE();

-- 1) 如果 difficulty 列不存在，则新增
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'questions'
    AND COLUMN_NAME = 'difficulty'
);

SET @sql_add := IF(
  @col_exists = 0,
  'ALTER TABLE questions ADD COLUMN difficulty VARCHAR(16) NULL COMMENT ''题目难度：简单/中等/困难'' AFTER type;',
  'SELECT ''[SKIP] questions.difficulty 已存在，跳过 ADD COLUMN'' AS message;'
);
PREPARE stmt_add FROM @sql_add;
EXECUTE stmt_add;
DEALLOCATE PREPARE stmt_add;

-- 2) 回填旧数据（列存在才执行）
SET @sql_fill := IF(
  @col_exists >= 0,
  'UPDATE questions SET difficulty = ''中等'' WHERE difficulty IS NULL OR difficulty = '''';',
  'SELECT ''[SKIP] questions 表不存在或无法读取列信息'' AS message;'
);
PREPARE stmt_fill FROM @sql_fill;
EXECUTE stmt_fill;
DEALLOCATE PREPARE stmt_fill;

-- 3) 设置默认值并改为非空（列存在才执行）
SET @sql_modify := IF(
  @col_exists >= 0,
  'ALTER TABLE questions MODIFY COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT ''中等'' COMMENT ''题目难度：简单/中等/困难'';',
  'SELECT ''[SKIP] questions 表不存在或无法读取列信息'' AS message;'
);
PREPARE stmt_modify FROM @sql_modify;
EXECUTE stmt_modify;
DEALLOCATE PREPARE stmt_modify;

-- 4) （可选）加一个简单约束：MySQL 8.0.16+ 才真正强制 CHECK
-- ALTER TABLE questions
--   ADD CONSTRAINT chk_questions_difficulty CHECK (difficulty IN ('简单','中等','困难'));
