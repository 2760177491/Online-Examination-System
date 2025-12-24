package com.example.onlineexam.service.impl;

import com.example.onlineexam.dto.*;
import com.example.onlineexam.entity.*;
import com.example.onlineexam.repository.*;
import com.example.onlineexam.service.TeacherSubjectiveGradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端：主观题批改实现
 */
@Service
public class TeacherSubjectiveGradingServiceImpl implements TeacherSubjectiveGradingService {

    private final StudentExamRepository studentExamRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public TeacherSubjectiveGradingServiceImpl(StudentExamRepository studentExamRepository,
                                              StudentAnswerRepository studentAnswerRepository,
                                              ExamSessionRepository examSessionRepository,
                                              ExamPaperRepository examPaperRepository,
                                              ExamQuestionRepository examQuestionRepository,
                                              QuestionRepository questionRepository,
                                              UserRepository userRepository) {
        this.studentExamRepository = studentExamRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.examSessionRepository = examSessionRepository;
        this.examPaperRepository = examPaperRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<TeacherSubjectivePendingItemDto> listPending(Long examSessionId, Long teacherId) {
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId 不能为空");
        }
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));

        // 安全：只能看自己创建的场次
        if (teacherId != null && session.getCreatedBy() != null && !session.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("无权限查看该考试场次");
        }

        // 查本场次所有 studentExam（已提交或已批改都算），然后筛出“存在未批改主观题”的记录
        List<StudentExam> studentExams = studentExamRepository.findByExamSessionId(examSessionId);
        if (studentExams.isEmpty()) return Collections.emptyList();

        // 预加载学生信息
        Set<Long> studentIds = studentExams.stream().map(StudentExam::getStudentId).collect(Collectors.toSet());
        Map<Long, User> userMap = studentIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(studentIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<TeacherSubjectivePendingItemDto> out = new ArrayList<>();
        for (StudentExam se : studentExams) {
            // 只关心已提交/已批改（避免“进行中”来批改）
            if (se.getStatus() == null) continue;
            if (!("已提交".equals(se.getStatus()) || "已批改".equals(se.getStatus()))) continue;

            // 统计该次考试中未批改的“主观题答案”数量
            int pending = countPendingSubjective(se.getId(), session.getExamPaperId());
            if (pending <= 0) continue;

            TeacherSubjectivePendingItemDto dto = new TeacherSubjectivePendingItemDto();
            dto.setStudentExamId(se.getId());
            dto.setExamSessionId(examSessionId);
            dto.setExamTitle(session.getName());
            dto.setStudentId(se.getStudentId());

            User u = userMap.get(se.getStudentId());
            dto.setStudentUsername(u == null ? "" : u.getUsername());
            dto.setStudentRealName(u == null ? "" : u.getRealName());

            dto.setSubmitTime(se.getSubmitTime());
            dto.setPendingCount(pending);
            dto.setCurrentTotalScore(se.getTotalScore() == null ? 0 : se.getTotalScore());
            out.add(dto);
        }

        // 按提交时间倒序
        out.sort((a, b) -> {
            if (a.getSubmitTime() == null && b.getSubmitTime() == null) return 0;
            if (a.getSubmitTime() == null) return 1;
            if (b.getSubmitTime() == null) return -1;
            return b.getSubmitTime().compareTo(a.getSubmitTime());
        });
        return out;
    }

    @Override
    public TeacherSubjectiveAnswerListDto getSubjectiveAnswers(Long studentExamId, Long teacherId) {
        StudentExam se = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));
        ExamSession session = examSessionRepository.findById(se.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        if (teacherId != null && session.getCreatedBy() != null && !session.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("无权限查看该考试场次");
        }

        ExamPaper paper = examPaperRepository.findById(session.getExamPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        User student = userRepository.findById(se.getStudentId()).orElse(null);

        // 加载试卷题目与分值/顺序
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamPaperIdOrderByOrderIndex(session.getExamPaperId());

        // 本次考试的全部答案
        List<StudentAnswer> answers = studentAnswerRepository.findByStudentExamId(studentExamId);
        Map<Long, StudentAnswer> answerMap = answers.stream().collect(Collectors.toMap(StudentAnswer::getQuestionId, a -> a, (a, b) -> a));

        // 只取主观题 questionId
        List<Long> qids = examQuestions.stream().map(ExamQuestion::getQuestionId).distinct().collect(Collectors.toList());
        Map<Long, Question> qMap = qids.isEmpty() ? Collections.emptyMap() : questionRepository.findAllById(qids)
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<TeacherSubjectiveAnswerItemDto> items = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question q = qMap.get(eq.getQuestionId());
            if (q == null) continue;
            if (!"subjective".equals(q.getType())) continue;

            StudentAnswer a = answerMap.get(eq.getQuestionId());
            if (a == null) continue; // 理论上提交时会有

            TeacherSubjectiveAnswerItemDto item = new TeacherSubjectiveAnswerItemDto();
            item.setStudentAnswerId(a.getId());
            item.setStudentExamId(studentExamId);
            item.setQuestionId(eq.getQuestionId());
            item.setOrderIndex(eq.getOrderIndex());
            item.setTitle(q.getTitle());
            item.setType(q.getType());
            item.setFullScore(eq.getScore());
            item.setReferenceAnswer(q.getCorrectAnswer());
            item.setStudentAnswer(a.getAnswerContent());
            item.setScore(a.getScore());
            item.setGradingStatus(a.getGradingStatus());
            items.add(item);
        }

        // 按题号排序
        items.sort(Comparator.comparing(i -> i.getOrderIndex() == null ? 999999 : i.getOrderIndex()));

        TeacherSubjectiveAnswerListDto dto = new TeacherSubjectiveAnswerListDto();
        dto.setStudentExamId(studentExamId);
        dto.setExamSessionId(se.getExamSessionId());
        dto.setExamTitle(session.getName());
        dto.setStudentId(se.getStudentId());
        dto.setStudentUsername(student == null ? "" : student.getUsername());
        dto.setStudentRealName(student == null ? "" : student.getRealName());
        dto.setSubmitTime(se.getSubmitTime());
        dto.setCurrentTotalScore(se.getTotalScore() == null ? 0 : se.getTotalScore());
        dto.setTotalScore(paper.getTotalScore() == null ? 0 : paper.getTotalScore());
        dto.setItems(items);
        return dto;
    }

    @Transactional
    @Override
    public TeacherSubjectiveAnswerListDto grade(TeacherGradeSubjectiveRequest req, Long teacherId) {
        if (req == null || req.getStudentExamId() == null) {
            throw new IllegalArgumentException("studentExamId 不能为空");
        }
        StudentExam se = studentExamRepository.findById(req.getStudentExamId())
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));
        ExamSession session = examSessionRepository.findById(se.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        if (teacherId != null && session.getCreatedBy() != null && !session.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("无权限批改该考试场次");
        }

        if (!("已提交".equals(se.getStatus()) || "已批改".equals(se.getStatus()))) {
            throw new RuntimeException("当前考试状态不允许批改：" + se.getStatus());
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            // 没传任何项，直接回显
            return getSubjectiveAnswers(req.getStudentExamId(), teacherId);
        }

        // 加载试卷题目（用于拿满分/过滤题型）
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamPaperIdOrderByOrderIndex(session.getExamPaperId());
        Map<Long, ExamQuestion> eqMap = examQuestions.stream().collect(Collectors.toMap(ExamQuestion::getQuestionId, x -> x, (a, b) -> a));
        List<Long> qids = examQuestions.stream().map(ExamQuestion::getQuestionId).distinct().collect(Collectors.toList());
        Map<Long, Question> qMap = qids.isEmpty() ? Collections.emptyMap() : questionRepository.findAllById(qids)
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        // 写入分数
        for (TeacherGradeSubjectiveRequest.Item item : req.getItems()) {
            if (item == null || item.getQuestionId() == null) continue;

            Question q = qMap.get(item.getQuestionId());
            if (q == null) continue;
            if (!"subjective".equals(q.getType())) continue;

            ExamQuestion eq = eqMap.get(item.getQuestionId());
            int full = eq == null || eq.getScore() == null ? (q.getScore() == null ? 0 : q.getScore()) : eq.getScore();

            Integer score = item.getScore();
            if (score == null) score = 0;
            if (score < 0) score = 0;
            if (full > 0 && score > full) score = full;

            StudentAnswer ans = studentAnswerRepository.findByStudentExamIdAndQuestionId(req.getStudentExamId(), item.getQuestionId());
            if (ans == null) continue;

            // 中文注释：教师批改后更新为已批改
            ans.setScore(score);
            ans.setGradingStatus("已批改");
            studentAnswerRepository.save(ans);
        }

        // 批改后：重算总分（客观题已经写入 score，主观题现在也写入了 score）
        int newTotal = recomputeTotalScore(req.getStudentExamId());
        se.setTotalScore(newTotal);

        // 如果主观题全部已批改，则考试记录标记为“已批改”，否则保持“已提交”
        int pending = countPendingSubjective(req.getStudentExamId(), session.getExamPaperId());
        se.setStatus(pending <= 0 ? "已批改" : "已提交");
        studentExamRepository.save(se);

        return getSubjectiveAnswers(req.getStudentExamId(), teacherId);
    }

    /**
     * 统计某次学生考试中：仍未批改的主观题数量
     */
    private int countPendingSubjective(Long studentExamId, Long examPaperId) {
        if (studentExamId == null || examPaperId == null) return 0;

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamPaperIdOrderByOrderIndex(examPaperId);
        if (examQuestions.isEmpty()) return 0;

        List<Long> qids = examQuestions.stream().map(ExamQuestion::getQuestionId).distinct().collect(Collectors.toList());
        if (qids.isEmpty()) return 0;
        Map<Long, Question> qMap = questionRepository.findAllById(qids).stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<StudentAnswer> answers = studentAnswerRepository.findByStudentExamId(studentExamId);
        int cnt = 0;
        for (StudentAnswer a : answers) {
            Question q = qMap.get(a.getQuestionId());
            if (q == null) continue;
            if (!"subjective".equals(q.getType())) continue;
            if (!"已批改".equals(a.getGradingStatus())) {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * 重算总分：把 student_answers.score 求和。
     * 中文注释：客观题由自动判分写入 score，主观题由教师批改写入 score。
     */
    private int recomputeTotalScore(Long studentExamId) {
        List<StudentAnswer> answers = studentAnswerRepository.findByStudentExamId(studentExamId);
        int sum = 0;
        for (StudentAnswer a : answers) {
            if (a.getScore() != null) {
                sum += a.getScore();
            }
        }
        return sum;
    }
}

