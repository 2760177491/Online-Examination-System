// API配置文件
const API_BASE_URL = "http://localhost:8080/api";

const api = {
    // 用户相关API
    USER_LOGIN: API_BASE_URL + "/users/login",
    USER_REGISTER: API_BASE_URL + "/users/register",

    // 题目相关API
    QUESTIONS: API_BASE_URL + "/questions",
    QUESTION: (id) => API_BASE_URL + "/questions/" + id,

    // 试卷相关API
    EXAMS: API_BASE_URL + "/exams",
    EXAM: (id) => API_BASE_URL + "/exams/" + id,

    // 考试相关API
    TAKE_EXAM: API_BASE_URL + "/exams/take",
    SUBMIT_EXAM: API_BASE_URL + "/exams/submit",

    // 成绩相关API
    RESULTS: API_BASE_URL + "/results",
    RESULT: (id) => API_BASE_URL + "/results/" + id,

    // 基础URL
    BASE_URL: API_BASE_URL,
};

export default api;