package com.example.onlineexam.service;

import com.example.onlineexam.entity.Question;
import com.example.onlineexam.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 题库业务逻辑
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question createQuestion(Question question) {
        // 创建题目时做一次输入校验，避免前端传脏数据
        normalizeAndValidate(question);
        return questionRepository.save(question);
    }

    /**
     * 查询题目列表（支持组合筛选）
     *
     * @param createdBy   出题教师ID（可为空）
     * @param type        题型（可为空）
     * @param keyword     题干关键字（可为空，支持包含匹配，忽略大小写）
     * @param difficulty  难度（可为空：简单/中等/困难）
     */
    public List<Question> listQuestions(Long createdBy, String type, String keyword, String difficulty) {
        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && kw.isEmpty()) kw = null;

        String t = type == null ? null : type.trim();
        if (t != null && t.isEmpty()) t = null;

        String d = normalizeDifficulty(difficulty);

        // 4个条件都存在：createdBy + type + difficulty + keyword
        if (createdBy != null && t != null && d != null && kw != null) {
            return questionRepository.findByCreatedByAndTypeAndDifficultyAndTitleContainingIgnoreCase(createdBy, t, d, kw);
        }

        // createdBy + type + difficulty
        if (createdBy != null && t != null && d != null) {
            return questionRepository.findByCreatedByAndTypeAndDifficulty(createdBy, t, d);
        }

        // createdBy + difficulty + keyword
        if (createdBy != null && d != null && kw != null) {
            return questionRepository.findByCreatedByAndDifficultyAndTitleContainingIgnoreCase(createdBy, d, kw);
        }

        // type + difficulty + keyword
        if (t != null && d != null && kw != null) {
            return questionRepository.findByTypeAndDifficultyAndTitleContainingIgnoreCase(t, d, kw);
        }

        // createdBy + type + keyword（无 difficulty）
        if (createdBy != null && t != null && kw != null) {
            return questionRepository.findByCreatedByAndTypeAndTitleContainingIgnoreCase(createdBy, t, kw);
        }

        // createdBy + type
        if (createdBy != null && t != null) {
            return questionRepository.findByCreatedByAndType(createdBy, t);
        }

        // createdBy + difficulty
        if (createdBy != null && d != null) {
            return questionRepository.findByCreatedByAndDifficulty(createdBy, d);
        }

        // type + difficulty
        if (t != null && d != null) {
            return questionRepository.findByTypeAndDifficulty(t, d);
        }

        // createdBy + keyword
        if (createdBy != null && kw != null) {
            return questionRepository.findByCreatedByAndTitleContainingIgnoreCase(createdBy, kw);
        }

        // difficulty + keyword
        if (d != null && kw != null) {
            return questionRepository.findByDifficultyAndTitleContainingIgnoreCase(d, kw);
        }

        // type + keyword
        if (t != null && kw != null) {
            return questionRepository.findByTypeAndTitleContainingIgnoreCase(t, kw);
        }

        // 只按 createdBy
        if (createdBy != null) {
            return questionRepository.findByCreatedBy(createdBy);
        }

        // 只按 type
        if (t != null) {
            return questionRepository.findByType(t);
        }

        // 只按 difficulty
        if (d != null) {
            return questionRepository.findByDifficulty(d);
        }

        // 只按 keyword
        if (kw != null) {
            return questionRepository.findByTitleContainingIgnoreCase(kw);
        }

        // 无条件：全量
        return questionRepository.findAll();
    }

    // 保持原有三参方法兼容
    public List<Question> listQuestions(Long createdBy, String type, String keyword) {
        return listQuestions(createdBy, type, keyword, null);
    }

    // 兼容旧调用：仍然支持只传 createdBy/type 的老方法
    public List<Question> listQuestions(Long createdBy, String type) {
        return listQuestions(createdBy, type, null, null);
    }

    public Question updateQuestion(Long id, Question payload) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));

        question.setTitle(payload.getTitle());
        question.setType(payload.getType());
        question.setOptionsJson(payload.getOptionsJson());
        question.setCorrectAnswer(payload.getCorrectAnswer());
        question.setScore(payload.getScore());
        question.setCreatedBy(payload.getCreatedBy());
        question.setDifficulty(payload.getDifficulty());

        // 更新同样需要校验
        normalizeAndValidate(question);
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    /**
     * 对题目进行“规范化 + 校验”。
     * 说明：
     * 1) 本项目题型统一使用：single_choice / multiple_choice / true_false / subjective
     * 2) 客观题（单选/多选/判断）必须有 optionsJson（JSON数组字符串）
     * 3) correctAnswer：
     *    - single_choice / true_false："A" 这种字母
     *    - multiple_choice："A,B" 这种逗号分隔字母，服务端会去重并按字母排序
     *    - subjective：可以是参考答案（目前数据库字段不允许为空）
     */
    private void normalizeAndValidate(Question q) {
        if (q == null) {
            throw new IllegalArgumentException("题目不能为空");
        }
        if (q.getTitle() == null || q.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("题干不能为空");
        }
        if (q.getType() == null || q.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("题型不能为空");
        }
        if (q.getScore() == null || q.getScore() <= 0) {
            throw new IllegalArgumentException("分值必须大于0");
        }
        if (q.getCreatedBy() == null) {
            throw new IllegalArgumentException("createdBy(教师ID)不能为空");
        }
        if (q.getCorrectAnswer() == null || q.getCorrectAnswer().trim().isEmpty()) {
            throw new IllegalArgumentException("正确答案不能为空");
        }

        // ============================
        // 难度字段校验（新增）
        // ============================
        q.setDifficulty(normalizeDifficulty(q.getDifficulty()));
        if (q.getDifficulty() == null) {
            // normalizeDifficulty 返回 null 仅代表传入为空；这里兜底默认“中等”
            q.setDifficulty("中等");
        }

        Set<String> allowedDifficulty = new HashSet<>(Arrays.asList("简单", "中等", "困难"));
        if (!allowedDifficulty.contains(q.getDifficulty())) {
            throw new IllegalArgumentException("难度必须是：简单 / 中等 / 困难");
        }

        String type = q.getType().trim();
        q.setType(type);

        Set<String> supported = new HashSet<>(Arrays.asList(
                "single_choice",
                "multiple_choice",
                "true_false",
                "subjective"
        ));
        if (!supported.contains(type)) {
            throw new IllegalArgumentException("不支持的题型：" + type);
        }

        if ("subjective".equals(type)) {
            // 主观题不要求选项
            return;
        }

        // 客观题必须有选项
        if (q.getOptionsJson() == null || q.getOptionsJson().trim().isEmpty()) {
            throw new IllegalArgumentException("客观题必须填写选项");
        }

        List<String> options;
        try {
            options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("optionsJson 不是合法JSON数组，例如：[\"A.xxx\",\"B.xxx\"]");
        }
        if (options == null) options = Collections.emptyList();
        if (options.size() < 2) {
            throw new IllegalArgumentException("选项至少需要2个");
        }

        // 判断题：强制约束为 A=正确，B=错误（避免老师输入不同文案导致学生端显示混乱）
        if ("true_false".equals(type)) {
            if (options.size() != 2) {
                throw new IllegalArgumentException("判断题选项必须是2个（A=正确，B=错误）");
            }
            // 给前端兜底：如果老师/前端传来的不是固定文案，服务端也强行统一
            q.setOptionsJson("[\"A.正确\",\"B.错误\"]");

            String a = q.getCorrectAnswer().trim().toUpperCase(Locale.ROOT);
            if (!"A".equals(a) && !"B".equals(a)) {
                throw new IllegalArgumentException("判断题正确答案必须是 A 或 B");
            }
            q.setCorrectAnswer(a);
            return;
        }

        // 计算“有效选项字母集合”：A,B,C,... 根据 options 数量推导
        Set<String> validLetters = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            validLetters.add(String.valueOf((char) ('A' + i)));
        }

        if ("single_choice".equals(type)) {
            String ans = q.getCorrectAnswer().trim().toUpperCase(Locale.ROOT);
            if (!validLetters.contains(ans)) {
                throw new IllegalArgumentException("单选题正确答案必须是：" + validLetters);
            }
            q.setCorrectAnswer(ans);
            return;
        }

        // 多选题：格式 A,B,C（顺序不重要，服务端会排序）
        if ("multiple_choice".equals(type)) {
            String raw = q.getCorrectAnswer().trim().toUpperCase(Locale.ROOT);
            String[] parts = raw.split(",");
            Set<String> selected = new TreeSet<>(); // TreeSet 自动排序
            for (String p : parts) {
                String s = p.trim();
                if (s.isEmpty()) continue;
                if (!validLetters.contains(s)) {
                    throw new IllegalArgumentException("多选题正确答案包含非法选项：" + s + "，合法范围：" + validLetters);
                }
                selected.add(s);
            }
            if (selected.isEmpty()) {
                throw new IllegalArgumentException("多选题至少选择1个正确选项");
            }
            q.setCorrectAnswer(String.join(",", selected));
        }
    }

    /**
     * 规范化难度：
     * - null/空字符串 => null（由调用方决定是否默认）
     * - 去掉首尾空格
     */
    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return null;
        String d = difficulty.trim();
        return d.isEmpty() ? null : d;
    }
}