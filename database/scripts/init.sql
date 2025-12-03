-- 在线考试系统数据库初始化脚本

-- 清空现有表（如果存在）
DROP TABLE IF EXISTS exam_questions;
DROP TABLE IF EXISTS exam_papers;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS users;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) UNIQUE NOT NULL,
    password     VARCHAR(255) NOT NULL,
    role         ENUM('TEACHER', 'STUDENT') NOT NULL,
    real_name    VARCHAR(50),
    email        VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建题目表
CREATE TABLE IF NOT EXISTS questions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(1000) NOT NULL,
    type           VARCHAR(32) NOT NULL,
    options_json   LONGTEXT,
    correct_answer VARCHAR(255) NOT NULL,
    score          INT NOT NULL,
    created_by     BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 创建试卷表
CREATE TABLE IF NOT EXISTS exam_papers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    total_score   INT NOT NULL,
    created_by    BIGINT NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 创建试卷题目关联表
CREATE TABLE IF NOT EXISTS exam_questions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_paper_id  BIGINT NOT NULL,
    question_id    BIGINT NOT NULL,
    order_index    INT NOT NULL,
    score          INT NOT NULL,
    FOREIGN KEY (exam_paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_exam_question (exam_paper_id, question_id)
);

-- 插入测试用户数据
-- 密码使用简单形式，实际应用中应使用加密存储
INSERT INTO users (username, password, role, real_name, email) VALUES
('teacher1', '123456', 'TEACHER', '张老师', 'teacher1@example.com'),
('teacher2', '123456', 'TEACHER', '李老师', 'teacher2@example.com'),
('student1', '123456', 'STUDENT', '王小华', 'student1@example.com'),
('student2', '123456', 'STUDENT', '李小强', 'student2@example.com'),
('student3', '123456', 'STUDENT', '陈小明', 'student3@example.com');

-- 插入测试题目数据 - 单选题
INSERT INTO questions (title, type, options_json, correct_answer, score, created_by) VALUES
('Java中，以下哪个是合法的标识符？', 'single_choice', '["A. 2variable", "B. _variable", "C. if", "D. +variable"]', 'B', 5, 1),
('Spring Boot的核心注解是什么？', 'single_choice', '["A. @Spring", "B. @Boot", "C. @SpringBootApplication", "D. @Application"]', 'C', 5, 1),
('MySQL中，哪个语句用于查询数据？', 'single_choice', '["A. SELECT", "B. GET", "C. FIND", "D. QUERY"]', 'A', 5, 2);

-- 插入测试题目数据 - 判断题
INSERT INTO questions (title, type, options_json, correct_answer, score, created_by) VALUES
('Vue.js是一个渐进式JavaScript框架。', 'true_false', '["A. 正确", "B. 错误"]', 'A', 3, 1),
('REST API总是返回JSON格式的数据。', 'true_false', '["A. 正确", "B. 错误"]', 'B', 3, 2),
('Java是一种解释型语言。', 'true_false', '["A. 正确", "B. 错误"]', 'B', 3, 1);

-- 插入测试题目数据 - 主观题
INSERT INTO questions (title, type, options_json, correct_answer, score, created_by) VALUES
('简述Spring MVC的工作流程。', 'subjective', NULL, '1. 客户端发送请求\n2. 前端控制器DispatcherServlet接收请求\n3. HandlerMapping查找处理器\n4. HandlerAdapter执行处理器\n5. 处理器返回ModelAndView\n6. 视图解析器解析视图\n7. 渲染视图并返回响应', 10, 1),
('解释什么是RESTful API及其设计原则。', 'subjective', NULL, 'RESTful API是遵循REST架构风格的API设计方式。\n设计原则包括：\n1. 资源导向\n2. 统一接口\n3. 无状态\n4. 缓存\n5. 分层系统\n6. 按需代码', 10, 2);

-- 插入测试试卷数据
INSERT INTO exam_papers (name, description, total_score, created_by) VALUES
('Java基础测试', 'Java语言基础知识测试，包含单选、判断和主观题', 100, 1),
('Web开发测试', 'Web开发相关知识测试，包含Spring、MySQL等内容', 100, 2);

-- 插入试卷题目关联数据
-- Java基础测试试卷题目
INSERT INTO exam_questions (exam_paper_id, question_id, order_index, score) VALUES
(1, 1, 1, 5),
(1, 4, 2, 3),
(1, 6, 3, 3),
(1, 7, 4, 10);

-- Web开发测试试卷题目
INSERT INTO exam_questions (exam_paper_id, question_id, order_index, score) VALUES
(2, 2, 1, 5),
(2, 3, 2, 5),
(2, 5, 3, 3),
(2, 8, 4, 10);

-- 更新试卷总分
UPDATE exam_papers ep 
SET total_score = (SELECT SUM(eq.score) FROM exam_questions eq WHERE eq.exam_paper_id = ep.id);

-- 查看插入的数据
SELECT 'Users created:' AS message;
SELECT id, username, role, real_name FROM users;

SELECT 'Questions created:' AS message;
SELECT id, title, type, score, created_by FROM questions;

SELECT 'Exam papers created:' AS message;
SELECT id, name, total_score, created_by FROM exam_papers;

SELECT 'Exam questions created:' AS message;
SELECT eq.id, eq.exam_paper_id, eq.question_id, eq.order_index, eq.score 
FROM exam_questions eq;

SELECT 'Database initialization completed successfully!' AS message;