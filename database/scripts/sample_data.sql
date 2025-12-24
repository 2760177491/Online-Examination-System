-- 表结构已经在其他脚本中创建，这里直接插入数据

-- 确保exam_questions表存在
CREATE TABLE IF NOT EXISTS exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_index INT NOT NULL,
    score INT NOT NULL,
    FOREIGN KEY (exam_paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 添加示例试卷（使用较大的id值避免冲突）
-- 先删除可能存在的试卷，确保name字段正确设置
DELETE FROM exam_questions WHERE exam_paper_id IN (3, 4);
DELETE FROM exam_sessions WHERE exam_paper_id IN (3, 4);
DELETE FROM exam_papers WHERE id IN (3, 4);

INSERT INTO exam_papers (id, name, description, total_score, created_by, created_at) VALUES
(3, '计算机基础测试卷', '这是一份计算机基础知识测试试卷', 50, 1, NOW()),
(4, 'Java编程测试卷', 'Java编程语言基础测试', 50, 1, NOW());

-- 关联试卷和题目（使用现有的题目ID）
-- 计算机基础测试卷关联4道题
INSERT INTO exam_questions (exam_paper_id, question_id, order_index, score) VALUES
(3, 1, 1, 5),  -- Java合法标识符题
(3, 4, 2, 3),  -- Vue.js判断题
(3, 6, 3, 3),  -- Java是解释型语言判断题
(3, 7, 4, 10); -- Spring MVC工作流程主观题

-- Java编程测试卷关联4道题
INSERT INTO exam_questions (exam_paper_id, question_id, order_index, score) VALUES
(4, 2, 1, 5),  -- Spring Boot核心注解题
(4, 3, 2, 5),  -- MySQL查询语句题
(4, 5, 3, 3),  -- REST API总是返回JSON判断题
(4, 8, 4, 10); -- RESTful API设计原则主观题

-- 更新试卷总分
UPDATE exam_papers ep 
SET total_score = (SELECT SUM(eq.score) FROM exam_questions eq WHERE eq.exam_paper_id = ep.id) 
WHERE ep.id IN (3, 4);

-- 添加示例考试场次
INSERT INTO exam_sessions (exam_paper_id, name, start_time, end_time, duration_minutes, status, created_by, created_at) VALUES
(3, '计算机基础期中考试', '2023-05-15 09:00:00', '2023-05-15 11:00:00', 120, '进行中', 1, NOW()),
(4, 'Java编程期末测试', '2023-06-20 14:00:00', '2023-06-20 16:00:00', 120, '未开始', 1, NOW()),
(3, '计算机基础补考', '2023-07-10 10:00:00', '2023-07-10 12:00:00', 120, '已结束', 1, NOW());

-- 添加示例学生考试记录（使用现有的学生ID：1,2,3）
INSERT INTO student_exams (exam_session_id, student_id, actual_start_time, submit_time, total_score, status) VALUES
(1, 1, '2023-05-15 09:00:00', '2023-05-15 10:30:00', 23, '已提交'),
(1, 2, '2023-05-15 09:00:00', NULL, NULL, '进行中'),
(3, 3, '2023-07-10 10:00:00', '2023-07-10 11:45:00', 18, '已提交');

-- 添加示例学生答案
INSERT INTO student_answers (student_exam_id, question_id, answer_content, score, grading_status) VALUES
-- 学生1的答案
(1, 1, 'B', 5, '已批改'),  -- Java合法标识符题
(1, 4, 'A', 3, '已批改'),  -- Vue.js判断题
(1, 6, 'B', 3, '已批改'),  -- Java是解释型语言判断题
(1, 7, 'Spring MVC工作流程包括：1.客户端发送请求；2.DispatcherServlet接收请求；3.HandlerMapping查找处理器；4.HandlerAdapter执行处理器；5.处理器返回ModelAndView；6.视图解析器解析视图；7.渲染视图并返回响应。', 12, '已批改'),  -- Spring MVC工作流程主观题

-- 学生3的答案
(3, 1, 'A', 0, '已批改'),  -- Java合法标识符题
(3, 4, 'A', 3, '已批改'),  -- Vue.js判断题
(3, 6, 'B', 3, '已批改'),  -- Java是解释型语言判断题
(3, 7, 'Spring MVC的工作流程是DispatcherServlet接收请求，然后找到合适的处理器，执行后返回结果。', 12, '已批改');  -- Spring MVC工作流程主观题