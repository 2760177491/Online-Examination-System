// API配置文件
const API_BASE_URL = "http://localhost:8080";

const api = {
    // 用户相关API
    USER_LOGIN: "/api/users/login",
    USER_REGISTER: "/api/users/register",

    // 题目相关API
    QUESTIONS: "/api/questions",
    QUESTION: (id) => "/api/questions/" + id,

    // 试卷相关API
    EXAMS: "/api/exams",
    EXAM: (id) => "/api/exams/" + id,

    // 考试相关API
    TAKE_EXAM: "/api/exams/take",
    SUBMIT_EXAM: "/api/exams/submit",

    // 成绩相关API
    RESULTS: "/api/results",
    RESULT: (id) => "/api/results/" + id,

    // 基础URL
    BASE_URL: API_BASE_URL,
};

export default api;