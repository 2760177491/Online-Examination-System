-- 测试用户数据初始化脚本
-- 在线考试系统仪表板测试用

-- 插入测试教师用户
INSERT INTO users (username, password, role, real_name, email) VALUES 
('teacher1', 'password123', 'TEACHER', '张老师', 'teacher1@example.com'),
('teacher2', 'password123', 'TEACHER', '李老师', 'teacher2@example.com');

-- 插入测试学生用户
INSERT INTO users (username, password, role, real_name, email) VALUES 
('student1', 'password123', 'STUDENT', '张三', 'student1@example.com'),
('student2', 'password123', 'STUDENT', '李四', 'student2@example.com'),
('student3', 'password123', 'STUDENT', '王五', 'student3@example.com');

-- 插入测试题目
INSERT INTO questions (type, content, options, answer, score, difficulty, created_by) VALUES 
('SINGLE_CHOICE', '以下哪个是Java的基本数据类型？', 
'{"A": "String", "B": "int", "C": "Integer", "D": "Object"}', 
'B', 5, 'EASY', 1),
('MULTIPLE_CHOICE', '以下哪些是面向对象编程的特性？', 
'{"A": "封装", "B": "继承", "C": "多态", "D": "过程化"}', 
'A,B,C', 10, 'MEDIUM', 1),
('TRUE_FALSE', 'Java是编译型语言。', 
'{"A": "正确", "B": "错误"}', 
'A', 3, 'EASY', 1);

-- 插入测试考试
INSERT INTO exams (title, description, duration, total_score, created_by) VALUES 
('Java基础测试', '测试Java基础知识', 60, 100, 1),
('数据结构期中考试', '测试数据结构基本概念', 90, 150, 2);

-- 关联考试和题目
INSERT INTO exam_questions (exam_id, question_id, question_order) VALUES 
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(2, 1, 1),
(2, 2, 2);