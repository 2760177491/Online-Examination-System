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
    EXAMS_WITH_QUESTIONS: "/api/exams/with-questions", // 创建试卷并选题组卷
    EXAM_WITH_QUESTIONS_UPDATE: (id) => `/api/exams/${id}/with-questions`, // 编辑试卷并重新组卷

    // 试卷题目关联（用于编辑页面回显已选题目）
    EXAM_QUESTIONS: "/api/exam-questions",

    // 考试相关API
    TAKE_EXAM: "/api/exams/take",
    SUBMIT_EXAM: "/api/exams/submit",

    // 成绩相关API
    RESULTS: "/api/results",
    RESULT: (id) => "/api/results/" + id,

    // 基础URL
    BASE_URL: API_BASE_URL,

    // ============================
    // 自动随机组卷（老师配置规则后由系统抽题并创建试卷）
    // ============================
    EXAMS_AUTO_ASSEMBLE: '/api/exams/auto-assemble',

    // ============================
    // 考试场次（ExamSession）相关接口
    // ============================
    EXAM_SESSIONS: '/api/exam-sessions',
    EXAM_SESSIONS_TEACHER: '/api/exam-sessions/teacher',
    EXAM_SESSIONS_CREATE_BY_PAPER: '/api/exam-sessions/create-by-paper',
    EXAM_SESSION: (id) => `/api/exam-sessions/${id}`,

    // ============================
    // 考试分配（ExamAssignment）
    // ============================
    USERS_STUDENTS: '/api/users/students',
    EXAM_ASSIGNMENTS: '/api/exam-assignments',
    EXAM_ASSIGNMENTS_MY: '/api/exam-assignments/my',
    EXAM_ASSIGNMENTS_COUNT: '/api/exam-assignments/count',
    EXAM_ASSIGNMENTS_CLEAR: '/api/exam-assignments/clear',
};

export default api;