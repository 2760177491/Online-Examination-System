package com.example.onlineexam.service.impl;

import com.example.onlineexam.dto.ResultDetailItemDto;
import com.example.onlineexam.dto.StudentExamListItemDto;
import com.example.onlineexam.dto.StudentResultDetailDto;
import com.example.onlineexam.dto.StudentResultListItemDto;
import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.entity.Question;
import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.entity.StudentAnswer;
import com.example.onlineexam.entity.ExamAssignment;
import com.example.onlineexam.repository.ExamPaperRepository;
import com.example.onlineexam.repository.StudentExamRepository;
import com.example.onlineexam.repository.StudentAnswerRepository;
import com.example.onlineexam.repository.ExamSessionRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.repository.ExamAssignmentRepository;
import com.example.onlineexam.service.StudentExamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生考试服务实现类
 */
@Service
public class StudentExamServiceImpl implements StudentExamService {

    @Autowired
    private StudentExamRepository studentExamRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private ExamPaperRepository examPaperRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAssignmentRepository examAssignmentRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public StudentExam startExam(Long studentId, Long examSessionId) {
        // 检查考试场次是否存在
        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));

        // ============================
        // 1) 考试分配校验：必须被分配才能参加
        // ============================
        boolean assigned = examAssignmentRepository.existsByExamSessionIdAndStudentId(examSessionId, studentId);
        if (!assigned) {
            throw new RuntimeException("该考试未分配给当前学生");
        }

        // ============================
        // 2) 考试时间/状态校验（以时间为准）
        // ============================
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(examSession.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (now.isAfter(examSession.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }

        // 检查学生是否已参加该考试
        StudentExam existingExam = studentExamRepository.findByStudentIdAndExamSessionId(studentId, examSessionId);
        if (existingExam != null) {
            // 已提交/已批改：禁止再次开始（前端也会禁用按钮，这里做服务端强校验）
            if ("已提交".equals(existingExam.getStatus()) || "已批改".equals(existingExam.getStatus())) {
                throw new RuntimeException("你已完成该考试，不能再次开始");
            }
            return existingExam;
        }

        // 创建新的学生考试记录
        StudentExam studentExam = new StudentExam();
        studentExam.setStudentId(studentId);
        studentExam.setExamSessionId(examSessionId);
        studentExam.setActualStartTime(now);
        studentExam.setStatus("进行中");
        studentExam.setCreatedAt(LocalDateTime.now());

        return studentExamRepository.save(studentExam);
    }

    @Override
    public StudentExam submitExam(Long studentExamId, List<StudentAnswer> answers) {
        // 获取学生考试记录
        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));

        // 检查考试状态，避免重复提交
        if ("已提交".equals(studentExam.getStatus()) || "已批改".equals(studentExam.getStatus())) {
            throw new RuntimeException("考试已提交，无法重复提交");
        }

        // 校验考试是否已结束（结束后禁止提交，避免出现“已结束仍能提交”的误解）
        ExamSession session = examSessionRepository.findById(studentExam.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(session.getEndTime())) {
            throw new RuntimeException("考试已结束，无法提交");
        }

        // ============================
        // 提交采用 upsert：
        // - 不再 deleteAll，避免把草稿清空
        // - 按题目维度更新/插入
        // ============================
        if (answers != null) {
            for (StudentAnswer answer : answers) {
                if (answer == null || answer.getQuestionId() == null) continue;

                // 强制设置为当前学生考试ID，防止前端篡改
                answer.setStudentExamId(studentExamId);
                answer.setGradingStatus("未批改");
                answer.setScore(null);

                StudentAnswer existed = studentAnswerRepository.findByStudentExamIdAndQuestionId(studentExamId, answer.getQuestionId());
                if (existed == null) {
                    // 新增
                    if (answer.getAnswerContent() == null) {
                        answer.setAnswerContent("");
                    }
                    studentAnswerRepository.save(answer);
                } else {
                    // 更新
                    existed.setAnswerContent(answer.getAnswerContent() == null ? "" : answer.getAnswerContent());
                    existed.setGradingStatus("未批改");
                    existed.setScore(null);
                    studentAnswerRepository.save(existed);
                }
            }
        }

        // 更新学生考试记录为“已提交”状态，记录提交时间
        studentExam.setSubmitTime(LocalDateTime.now());
        studentExam.setStatus("已提交");

        return studentExamRepository.save(studentExam);
    }

    @Override
    public StudentExam saveDraft(Long studentExamId, List<StudentAnswer> answers) {
        if (studentExamId == null) {
            throw new IllegalArgumentException("studentExamId 不能为空");
        }

        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));

        // 仅允许进行中保存草稿
        if (!"进行中".equals(studentExam.getStatus())) {
            throw new RuntimeException("当前考试状态不允许保存草稿");
        }

        // 超时也不允许保存草稿（防止结束后还能修改答案）
        ExamSession session = examSessionRepository.findById(studentExam.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        if (LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new RuntimeException("考试已结束，无法保存草稿");
        }

        if (answers == null) {
            return studentExam;
        }

        // 按题目维度 upsert
        for (StudentAnswer a : answers) {
            if (a == null || a.getQuestionId() == null) continue;
            StudentAnswer existed = studentAnswerRepository.findByStudentExamIdAndQuestionId(studentExamId, a.getQuestionId());
            if (existed == null) {
                StudentAnswer na = new StudentAnswer();
                na.setStudentExamId(studentExamId);
                na.setQuestionId(a.getQuestionId());
                na.setAnswerContent(a.getAnswerContent() == null ? "" : a.getAnswerContent());
                na.setScore(null);
                na.setGradingStatus("未批改");
                studentAnswerRepository.save(na);
            } else {
                existed.setAnswerContent(a.getAnswerContent() == null ? "" : a.getAnswerContent());
                // 草稿阶段不评分
                existed.setScore(null);
                existed.setGradingStatus("未批改");
                studentAnswerRepository.save(existed);
            }
        }

        return studentExam;
    }

    @Override
    public StudentExam getStudentExamById(Long id) {
        return studentExamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));
    }

    @Override
    public List<StudentExam> getStudentExamsByStudentId(Long studentId) {
        return studentExamRepository.findByStudentId(studentId);
    }

    @Override
    public List<StudentExam> getStudentExamsByExamSessionId(Long examSessionId) {
        return studentExamRepository.findByExamSessionId(examSessionId);
    }

    @Override
    public StudentAnswer saveStudentAnswer(StudentAnswer answer) {
        answer.setGradingStatus("未批改");
        return studentAnswerRepository.save(answer);
    }

    @Override
    public List<StudentAnswer> getStudentAnswersByStudentExamId(Long studentExamId) {
        return studentAnswerRepository.findByStudentExamId(studentExamId);
    }

    @Override
    public List<StudentExamListItemDto> getMyExamList(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }

        // ============================
        // 关键改造：学生“我的考试列表”只展示“已分配给我的场次”
        // ============================
        List<ExamAssignment> assignments = examAssignmentRepository.findByStudentId(studentId);
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> sessionIds = assignments.stream()
                .map(a -> a.getExamSessionId())
                .collect(Collectors.toSet());
        List<ExamSession> sessions = examSessionRepository.findAllById(sessionIds);

        // 学生已参加记录
        List<StudentExam> my = studentExamRepository.findByStudentId(studentId);
        Map<Long, StudentExam> bySessionId = new HashMap<>();
        for (StudentExam se : my) {
            if (se == null || se.getExamSessionId() == null) continue;
            StudentExam existed = bySessionId.get(se.getExamSessionId());
            if (existed == null) {
                bySessionId.put(se.getExamSessionId(), se);
                continue;
            }
            LocalDateTime t1 = existed.getSubmitTime();
            LocalDateTime t2 = se.getSubmitTime();
            if (t1 == null || (t2 != null && t2.isAfter(t1))) {
                bySessionId.put(se.getExamSessionId(), se);
            }
        }

        List<StudentExamListItemDto> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ExamSession s : sessions) {
            StudentExamListItemDto dto = new StudentExamListItemDto();
            dto.setExamSessionId(s.getId());
            dto.setExamTitle(s.getName());
            dto.setDurationMinutes(s.getDurationMinutes());
            dto.setExamPaperId(s.getExamPaperId());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());

            StudentExam se = bySessionId.get(s.getId());
            if (se != null && ("已提交".equals(se.getStatus()) || "已批改".equals(se.getStatus()))) {
                dto.setStatus("已完成");
            } else {
                // 以时间为准计算未开始/进行中/已完成
                if (now.isBefore(s.getStartTime())) {
                    dto.setStatus("未开始");
                } else if (now.isAfter(s.getEndTime())) {
                    dto.setStatus("已完成");
                } else {
                    dto.setStatus("进行中");
                }
            }
            list.add(dto);
        }

        // 按开始时间倒序
        list.sort((a, b) -> {
            LocalDateTime t1 = a.getStartTime();
            LocalDateTime t2 = b.getStartTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        return list;
    }

    @Override
    public List<StudentResultListItemDto> getMyResultList(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }

        // 1) 取学生所有考试记录（只显示已提交/已批改 的作为“成绩”）
        List<StudentExam> my = studentExamRepository.findByStudentId(studentId);
        List<StudentExam> submitted = my.stream()
                .filter(se -> se != null && ("已提交".equals(se.getStatus()) || "已批改".equals(se.getStatus())))
                .collect(Collectors.toList());

        if (submitted.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 批量查考试场次，拿到考试标题 和 examPaperId
        Set<Long> sessionIds = submitted.stream().map(StudentExam::getExamSessionId).collect(Collectors.toSet());
        List<ExamSession> sessions = examSessionRepository.findAllById(sessionIds);
        Map<Long, ExamSession> sessionMap = sessions.stream().collect(Collectors.toMap(ExamSession::getId, x -> x));

        // 3) 批量查试卷，拿到 totalScore（试卷总分）
        Set<Long> paperIds = sessions.stream().map(ExamSession::getExamPaperId).collect(Collectors.toSet());
        List<ExamPaper> papers = examPaperRepository.findAllById(paperIds);
        Map<Long, ExamPaper> paperMap = papers.stream().collect(Collectors.toMap(ExamPaper::getId, x -> x));

        // 4) 组装 DTO
        List<StudentResultListItemDto> result = new ArrayList<>();
        for (StudentExam se : submitted) {
            ExamSession s = sessionMap.get(se.getExamSessionId());
            ExamPaper p = (s == null) ? null : paperMap.get(s.getExamPaperId());

            StudentResultListItemDto dto = new StudentResultListItemDto();
            dto.setStudentExamId(se.getId());
            dto.setExamSessionId(se.getExamSessionId());
            dto.setExamTitle(s != null ? s.getName() : "");
            dto.setScore(se.getTotalScore() == null ? 0 : se.getTotalScore());
            dto.setTotalScore(p != null && p.getTotalScore() != null ? p.getTotalScore() : 0);
            dto.setSubmitTime(se.getSubmitTime());
            result.add(dto);
        }

        // 5) 按提交时间倒序
        result.sort((a, b) -> {
            LocalDateTime t1 = a.getSubmitTime();
            LocalDateTime t2 = b.getSubmitTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return result;
    }

    /**
     * 题型英文 -> 中文显示
     */
    private String toTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "single_choice" -> "单选题";
            case "multiple_choice" -> "多选题";
            case "true_false" -> "判断题";
            case "subjective" -> "简答题";
            default -> type;
        };
    }

    /**
     * 将 optionsJson 解析为 Map<选项字母, 选项文本>
     * 兼容两种历史格式：
     * 1) Map：{"A":"xxx","B":"yyy"}
     * 2) List：["A.xxx","B.yyy"]
     */
    private Map<String, String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // 先按通用 Object 解析，判断是 Map 还是 List
            Object parsed = OBJECT_MAPPER.readValue(optionsJson, Object.class);
            if (parsed instanceof Map<?, ?> m) {
                Map<String, String> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() == null) continue;
                    String k = String.valueOf(e.getKey()).trim();
                    String v = e.getValue() == null ? "" : String.valueOf(e.getValue());
                    if (!k.isEmpty()) {
                        result.put(k, v);
                    }
                }
                return result;
            }

            if (parsed instanceof List<?> list) {
                // List 形如："A.xxx" / "B.yyy" / ...
                Map<String, String> result = new LinkedHashMap<>();
                for (Object o : list) {
                    if (o == null) continue;
                    String s = String.valueOf(o).trim();
                    if (s.isEmpty()) continue;

                    // 预期格式：A.xxx 或 A:xxx
                    String label = null;
                    String text = null;

                    // 优先处理 A.xxx
                    int dot = s.indexOf('.');
                    if (dot > 0) {
                        label = s.substring(0, dot).trim();
                        text = s.substring(dot + 1).trim();
                    } else {
                        // 兼容 A:xxx
                        int colon = s.indexOf(':');
                        if (colon > 0) {
                            label = s.substring(0, colon).trim();
                            text = s.substring(colon + 1).trim();
                        }
                    }

                    if (label != null && !label.isEmpty()) {
                        result.put(label, text);
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
            // 解析失败则兜底为空
        }
        return Collections.emptyMap();
    }

    /**
     * 学生答案“文本/字母”归一化：
     * - 如果传来的是 A/B/C... 则直接返回
     * - 如果传来的是选项文本，则在 optionsMap 里反查得到字母
     */
    private String normalizeObjectiveAnswer(String raw, Map<String, String> optionsMap) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";

        // 已经是字母（A/B/C/D/E/F...）
        if (s.matches("^[A-Z]$") || s.matches("^[A-Z](,[A-Z])+$")) {
            return s;
        }

        // 判断题可能是 “正确/错误”
        if ("正确".equals(s) || "错误".equals(s)) {
            // 尝试从 optionsMap 反查到 A/B
            for (Map.Entry<String, String> e : optionsMap.entrySet()) {
                if (s.equals(e.getValue())) {
                    return e.getKey();
                }
            }
            // 如果没有 optionsJson，则用约定：正确->A，错误->B
            return "正确".equals(s) ? "A" : "B";
        }

        // 用选项文本反查
        for (Map.Entry<String, String> e : optionsMap.entrySet()) {
            if (s.equals(e.getValue())) {
                return e.getKey();
            }
        }

        // 兜底：返回原值
        return s;
    }

    /** 多选题答案归一化：把文本或字母统一转为 A,B,C 形式并排序 */
    private String normalizeMultipleAnswer(String raw, Map<String, String> optionsMap) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";

        // 如果后端存的是 JSON 数组或其他格式，这里也尽量兼容
        // 统一按逗号拆分
        String[] parts = s.split(",");
        List<String> letters = new ArrayList<>();
        for (String p : parts) {
            String one = p.trim();
            if (one.isEmpty()) continue;
            String normalized = normalizeObjectiveAnswer(one, optionsMap);
            // 如果是单字母，收集；否则仍收集原值
            letters.add(normalized);
        }

        // 排序：尽量按字母序
        letters.sort(String::compareTo);
        return String.join(",", letters);
    }

    @Override
    public StudentResultDetailDto getResultDetail(Long studentExamId) {
        if (studentExamId == null) {
            throw new IllegalArgumentException("studentExamId 不能为空");
        }

        // 1) 考试记录（student_exams）
        StudentExam se = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));

        // 2) 考试场次（exam_sessions）
        ExamSession session = examSessionRepository.findById(se.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));

        // 3) 试卷（exam_papers）
        ExamPaper paper = examPaperRepository.findById(session.getExamPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        // 4) 学生答案（student_answers）
        List<StudentAnswer> answers = studentAnswerRepository.findByStudentExamId(studentExamId);

        // 5) 批量查题库题目（questions）
        Set<Long> qids = answers.stream().map(StudentAnswer::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = qids.isEmpty() ? Collections.emptyList() : questionRepository.findAllById(qids);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, x -> x));

        // 6) 组装详情
        StudentResultDetailDto dto = new StudentResultDetailDto();
        dto.setStudentExamId(se.getId());
        dto.setExamSessionId(se.getExamSessionId());
        dto.setExamTitle(session.getName());
        dto.setScore(se.getTotalScore() == null ? 0 : se.getTotalScore());
        dto.setTotalScore(paper.getTotalScore() == null ? 0 : paper.getTotalScore());

        List<ResultDetailItemDto> items = new ArrayList<>();
        for (StudentAnswer a : answers) {
            Question q = questionMap.get(a.getQuestionId());

            ResultDetailItemDto item = new ResultDetailItemDto();
            item.setQuestionId(a.getQuestionId());
            item.setScore(a.getScore());
            item.setGradingStatus(a.getGradingStatus());

            if (q != null) {
                item.setTitle(q.getTitle());
                item.setType(q.getType());
                item.setTypeLabel(toTypeLabel(q.getType()));
                item.setQuestionScore(q.getScore());

                // 选项（用于前端可选展示，也用于本次“答案归一化”）
                item.setOptionsJson(q.getOptionsJson());
                Map<String, String> optionsMap = parseOptions(q.getOptionsJson());

                // 正确答案：主观题显示为“参考答案”，客观题显示为字母（题库里就是字母/逗号）
                if ("subjective".equals(q.getType())) {
                    item.setCorrectAnswer(q.getCorrectAnswer()); // 参考答案
                    item.setStudentAnswer(a.getAnswerContent());
                    item.setCorrect(null); // 主观题是否正确需要人工/更复杂策略
                } else if ("multiple_choice".equals(q.getType())) {
                    String normalizedStudent = normalizeMultipleAnswer(a.getAnswerContent(), optionsMap);
                    item.setStudentAnswer(normalizedStudent);
                    item.setCorrectAnswer(q.getCorrectAnswer());

                    // 客观题对错判断：忽略顺序
                    String ca = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().replace(" ", "");
                    Set<String> correctSet = new HashSet<>(Arrays.asList(ca.split(",")));
                    Set<String> studentSet = new HashSet<>(Arrays.asList(normalizedStudent.split(",")));
                    item.setCorrect(!correctSet.isEmpty() && correctSet.equals(studentSet));
                } else {
                    // single_choice / true_false
                    String normalizedStudent = normalizeObjectiveAnswer(a.getAnswerContent(), optionsMap);
                    item.setStudentAnswer(normalizedStudent);
                    item.setCorrectAnswer(q.getCorrectAnswer());

                    String ca = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().trim();
                    item.setCorrect(!ca.isEmpty() && ca.equalsIgnoreCase(normalizedStudent));
                }
            } else {
                item.setTitle("(题目已删除)");
                item.setType("unknown");
                item.setTypeLabel("未知");
                item.setQuestionScore(0);
                item.setCorrectAnswer("");
                item.setStudentAnswer(a.getAnswerContent());
                item.setCorrect(null);
                item.setOptionsJson(null);
            }
            items.add(item);
        }

        // 可选：按 questionId 排序，保证展示稳定
        items.sort(Comparator.comparing(ResultDetailItemDto::getQuestionId));
        dto.setItems(items);
        return dto;
    }
}

