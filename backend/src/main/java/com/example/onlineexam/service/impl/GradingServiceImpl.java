package com.example.onlineexam.service.impl;

import com.example.onlineexam.dto.GradingResultDto;
import com.example.onlineexam.dto.QuestionScoreDto;
import com.example.onlineexam.entity.*;
import com.example.onlineexam.repository.*;
import com.example.onlineexam.service.GradingService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自动判分服务实现类
 */
@Service
public class GradingServiceImpl implements GradingService {

    private final StudentExamRepository studentExamRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ExamSessionRepository examSessionRepository;
    // private final ExamPaperRepository examPaperRepository; // 未使用，移除
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;

    public GradingServiceImpl(StudentExamRepository studentExamRepository,
                              StudentAnswerRepository studentAnswerRepository,
                              ExamSessionRepository examSessionRepository,
                              /*ExamPaperRepository examPaperRepository,*/
                              ExamQuestionRepository examQuestionRepository,
                              QuestionRepository questionRepository) {
        this.studentExamRepository = studentExamRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.examSessionRepository = examSessionRepository;
        // this.examPaperRepository = examPaperRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public GradingResultDto autoGrade(Long studentExamId) {
        // 1. 查找学生考试记录
        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));

        // 2. 根据考试场次找到对应试卷
        ExamSession examSession = examSessionRepository.findById(studentExam.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        Long examPaperId = examSession.getExamPaperId();

        // 3. 查询试卷中的所有题目（带顺序和分值）
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamPaperIdOrderByOrderIndex(examPaperId);
        if (examQuestions.isEmpty()) {
            throw new RuntimeException("试卷下没有题目，无法判分");
        }

        // 4. 查询本次考试的所有学生答案，并按 questionId 建立映射
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByStudentExamId(studentExamId);
        Map<Long, StudentAnswer> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, a -> a, (a1, a2) -> a1));

        // 5. 查询所有涉及的题目实体，并建立 questionId -> Question 的映射
        List<Long> questionIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        List<Question> questions = questionRepository.findAllById(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        List<QuestionScoreDto> questionScoreDtos = new ArrayList<>();

        // 6. 遍历试卷上的每一道题，进行逐题判分
        for (ExamQuestion examQuestion : examQuestions) {
            Long qid = examQuestion.getQuestionId();
            Question question = questionMap.get(qid);
            if (question == null) {
                // 数据不一致：试卷引用了不存在的题目，直接跳过
                continue;
            }

            StudentAnswer answer = answerMap.get(qid);
            String studentAnswerContent = (answer != null) ? answer.getAnswerContent() : null;

            int fullScore = examQuestion.getScore();
            int obtainedScore = gradeQuestion(question, studentAnswerContent, fullScore);

            // 更新学生答案记录中的得分和批改状态
            if (answer != null) {
                answer.setScore(obtainedScore);
                // 客观题自动批改为“已批改”，主观题保留原状态
                if (isObjectiveQuestion(question.getType())) {
                    answer.setGradingStatus("已批改");
                }
            }

            totalScore += obtainedScore;

            // 组装单题得分 DTO
            QuestionScoreDto qs = new QuestionScoreDto();
            qs.setQuestionId(qid);
            qs.setOrderIndex(examQuestion.getOrderIndex());
            qs.setQuestionScore(fullScore);
            qs.setObtainedScore(obtainedScore);
            qs.setQuestionType(question.getType());
            qs.setCorrectAnswer(question.getCorrectAnswer());
            qs.setStudentAnswer(studentAnswerContent);
            questionScoreDtos.add(qs);
        }

        // 7. 批量保存更新后的学生答案
        studentAnswerRepository.saveAll(studentAnswers);

        // ============================
        // 8. 更新 student_exams 状态
        // ============================
        // 中文注释：自动判分只处理客观题。
        // - 如果存在主观题答案且还未批改，则 StudentExam 应保持“已提交”。
        // - 只有当不存在“未批改主观题”时，才标记为“已批改”。
        boolean hasUnGradedSubjective = false;
        for (ExamQuestion eq : examQuestions) {
            Question q = questionMap.get(eq.getQuestionId());
            if (q == null) continue;
            if (!"subjective".equals(q.getType())) continue;

            StudentAnswer a = answerMap.get(eq.getQuestionId());
            if (a == null) {
                // 有主观题但没有答案：按未批改处理
                hasUnGradedSubjective = true;
                break;
            }
            if (!"已批改".equals(a.getGradingStatus())) {
                hasUnGradedSubjective = true;
                break;
            }
        }

        studentExam.setStatus(hasUnGradedSubjective ? "已提交" : "已批改");
        studentExamRepository.save(studentExam);

        // 9. 封装判分结果 DTO，返回给前端
        GradingResultDto resultDto = new GradingResultDto();
        resultDto.setStudentExamId(studentExamId);
        resultDto.setTotalScore(totalScore);
        resultDto.setTotalQuestions(questionScoreDtos.size());
        resultDto.setQuestionScores(questionScoreDtos);

        return resultDto;
    }

    /**
     * 根据题目类型分发到不同的判分函数
     */
    private int gradeQuestion(Question question, String studentAnswerRaw, int fullScore) {
        String type = question.getType();
        if (type == null) {
            return 0;
        }
        switch (type) {
            case "single_choice":
                // 单选题需要结合 optionsJson，将学生提交的“选项文字”还原为选项字母
                return gradeSingleChoice(question, studentAnswerRaw, fullScore);
            case "multiple_choice":
                return gradeMultipleChoice(question.getCorrectAnswer(), studentAnswerRaw, fullScore);
            case "true_false":
                return gradeTrueFalse(question.getCorrectAnswer(), studentAnswerRaw, fullScore);
            default:
                // 主观题或其他类型暂不自动判分，返回 0 分
                return 0;
        }
    }

    /**
     * 单选题判分：
     * 1. 如果学生提交的是 A/B/C/D 这样的选项字母，直接与 correctAnswer 比较；
     * 2. 如果学生提交的是选项文字（例如 "_variable"），
     *    则根据 Question.optionsJson 反推它对应的选项字母（A/B/C...），再与 correctAnswer 比较。
     */
    private int gradeSingleChoice(Question question, String studentAnswerRaw, int fullScore) {
        String correct = question.getCorrectAnswer(); // 例如 "B"
        if (correct == null || correct.isEmpty()) {
            return 0;
        }
        if (studentAnswerRaw == null || studentAnswerRaw.trim().isEmpty()) {
            return 0;
        }

        String normalizedCorrect = normalizeOption(correct); // B

        // 1. 先判断学生答案是否本身就是选项字母（A/B/C/D）
        String normalizedStudent = normalizeOption(studentAnswerRaw);
        if (normalizedStudent != null
                && normalizedStudent.length() == 1
                && normalizedStudent.charAt(0) >= 'A'
                && normalizedStudent.charAt(0) <= 'Z') {
            return normalizedStudent.equalsIgnoreCase(normalizedCorrect) ? fullScore : 0;
        }

        // 2. 学生提交的不是字母，而是选项文字，尝试在 optionsJson 中找到对应的选项字母
        String selectedLabel = resolveLabelFromOptions(question.getOptionsJson(), studentAnswerRaw);
        if (selectedLabel == null) {
            // 没有在选项中找到对应文字，判 0 分
            return 0;
        }

        String normalizedLabel = normalizeOption(selectedLabel); // 例如 "B"
        return normalizedLabel.equalsIgnoreCase(normalizedCorrect) ? fullScore : 0;
    }

    /**
     * 多选题判分：默认策略为“全对得满分，否则 0 分”
     * 若后续需要改为“部分正确给部分分”，只需在此方法内调整逻辑即可。
     */
    private int gradeMultipleChoice(String correct, String student, int fullScore) {
        if (correct == null || correct.isEmpty()) {
            return 0;
        }
        Set<String> correctSet = splitToSet(correct);
        Set<String> studentSet = splitToSet(student);
        if (studentSet.isEmpty()) {
            return 0;
        }
        return correctSet.equals(studentSet) ? fullScore : 0;
    }

    /**
     * 判断题判分：true/false 或 T/F，忽略大小写
     */
    private int gradeTrueFalse(String correct, String student, int fullScore) {
        if (correct == null || correct.isEmpty()) {
            return 0;
        }
        Boolean c = toBoolean(correct);
        Boolean s = toBoolean(student);
        if (c == null || s == null) {
            return 0;
        }
        return c.equals(s) ? fullScore : 0;
    }

    /**
     * 判断是否为客观题（可以自动判分的题型）
     */
    private boolean isObjectiveQuestion(String type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case "single_choice":
            case "multiple_choice":
            case "true_false":
                return true;
            default:
                return false;
        }
    }

    /**
     * 将类似 "A"、" a " 这样的选项规范化（去空格并转大写）
     */
    private String normalizeOption(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase();
    }

    /**
     * 将 "A,B,C" 这样的多选答案拆分为去重后的大写集合
     */
    private Set<String> splitToSet(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String[] parts = s.split(",");
        Set<String> set = new LinkedHashSet<>();
        for (String part : parts) {
            String opt = normalizeOption(part);
            if (opt != null) {
                set.add(opt);
            }
        }
        return set;
    }

    /**
     * 从题目的 optionsJson 中，根据学生提交的“选项文字”反推出对应的选项字母（A/B/C...）。
     * 兼容两种格式：
     * 1. 标准 JSON 数组，例如 ["A. 2variable", "B. _variable", ...]
     * 2. 简单用逗号分隔的字符串，例如 "A. 文本1,B. 文本2"
     */
    private String resolveLabelFromOptions(String optionsJson, String studentAnswerRaw) {
        if (optionsJson == null || optionsJson.trim().isEmpty() || studentAnswerRaw == null) {
            return null;
        }
        String target = studentAnswerRaw.trim();

        // 尝试按 JSON 数组解析
        try {
            // 解析为 Object 数组，然后逐个处理
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(optionsJson, Object.class);
            if (parsed instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) parsed;
                for (int i = 0; i < list.size(); i++) {
                    String text = String.valueOf(list.get(i)); // 例如 "A. _variable"
                    // 按第一个 '.' 分割前缀（选项字母）和内容
                    String[] parts = text.split("\\.", 2);
                    String label = parts.length > 0 ? parts[0].trim() : String.valueOf((char) ('A' + i));
                    String value = parts.length > 1 ? parts[1].trim() : text.trim();

                    // 严格匹配学生提交的文本
                    if (value.equals(target)) {
                        return label;
                    }
                }
            }
        } catch (Exception e) {
            // 如果不是合法的 JSON，进入兜底逻辑
        }

        // 兜底：按逗号分隔，再按 '.' 分割 label 和 value
        String[] items = optionsJson.split(",");
        for (int i = 0; i < items.length; i++) {
            String text = items[i].trim();
            String[] parts = text.split("\\.", 2);
            String label = parts.length > 0 ? parts[0].trim() : String.valueOf((char) ('A' + i));
            String value = parts.length > 1 ? parts[1].trim() : text.trim();

            if (value.equals(target)) {
                return label;
            }
        }

        return null;
    }

    /**
     * 将 true/false 或 T/F 以及常见中文“是/否/正确/错误/对/不对”等转换为 Boolean
     * 这样可以兼容题库中使用 A/B 代表“正确/错误”，以及学生直接填写“正确/错误”的情况。
     */
    private Boolean toBoolean(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim().toLowerCase();

        // 表示“正确 / 是 / 对”
        if ("true".equals(v)
                || "t".equals(v)
                || "1".equals(v)
                || "是".equals(v)
                || "正确".equals(v)
                || "对".equals(v)
                || "a".equals(v)) { // 兼容 A 选项代表“正确”的情况
            return Boolean.TRUE;
        }

        // 表示“错误 / 否 / 不对”
        if ("false".equals(v)
                || "f".equals(v)
                || "0".equals(v)
                || "否".equals(v)
                || "错误".equals(v)
                || "不对".equals(v)
                || "b".equals(v)) { // 兼容 B 选项代表“错误”的情况
            return Boolean.FALSE;
        }

        return null;
    }
}
