-- 创建考试场次表
CREATE TABLE IF NOT EXISTS exam_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_paper_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建学生考试记录表
CREATE TABLE IF NOT EXISTS student_exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    actual_start_time DATETIME NOT NULL,
    submit_time DATETIME,
    total_score INT,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建学生答案表
CREATE TABLE IF NOT EXISTS student_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_content VARCHAR(1000) NOT NULL,
    score INT,
    grading_status VARCHAR(32) NOT NULL
);

-- 添加外键约束（可选，根据需要添加）
-- ALTER TABLE exam_sessions ADD CONSTRAINT fk_exam_session_paper FOREIGN KEY (exam_paper_id) REFERENCES exam_papers(id);
-- ALTER TABLE student_exams ADD CONSTRAINT fk_student_exam_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id);
-- ALTER TABLE student_answers ADD CONSTRAINT fk_student_answer_exam FOREIGN KEY (student_exam_id) REFERENCES student_exams(id);