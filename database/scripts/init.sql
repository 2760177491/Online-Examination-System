-- 用户表
CREATE TABLE IF NOT EXISTS users
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) UNIQUE          NOT NULL,
    password     VARCHAR(255)                NOT NULL,
    role         ENUM ('TEACHER', 'STUDENT') NOT NULL,
    real_name    VARCHAR(50),
    email        VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 题目表
CREATE TABLE IF NOT EXISTS questions
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    type         ENUM ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE') NOT NULL,
    content      TEXT                                                    NOT NULL,
    options      JSON,
    answer       TEXT                                                    NOT NULL,
    score        INT                             DEFAULT 1,
    difficulty   ENUM ('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    created_by   BIGINT,
    created_time TIMESTAMP                       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users (id)
);

-- 试卷表
CREATE TABLE IF NOT EXISTS exams
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    duration     INT          NOT NULL, -- 考试时长(分钟)
    total_score  INT          NOT NULL,
    created_by   BIGINT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users (id)
);

-- 试卷题目关联表
CREATE TABLE IF NOT EXISTS exam_questions
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id        BIGINT,
    question_id    BIGINT,
    question_order INT,
    FOREIGN KEY (exam_id) REFERENCES exams (id),
    FOREIGN KEY (question_id) REFERENCES questions (id)
);