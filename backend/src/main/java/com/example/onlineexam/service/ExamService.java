package com.example.onlineexam.service;

import com.example.onlineexam.dto.AutoAssembleExamPaperRequest;
import com.example.onlineexam.dto.CreateExamPaperRequest;
import com.example.onlineexam.dto.UpdateExamPaperRequest;
import com.example.onlineexam.dto.AutoAssembleRuleItem;
import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.entity.ExamQuestion;
import com.example.onlineexam.entity.Question;
import com.example.onlineexam.repository.ExamPaperRepository;
import com.example.onlineexam.repository.ExamQuestionRepository;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 试卷业务逻辑
 */
@Service
public class ExamService {

    private final ExamPaperRepository examPaperRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;

    public ExamService(ExamPaperRepository examPaperRepository,
                       ExamQuestionRepository examQuestionRepository,
                       QuestionRepository questionRepository) {
        this.examPaperRepository = examPaperRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * 创建试卷
     */
    public ExamPaper createExamPaper(ExamPaper examPaper) {
        return examPaperRepository.save(examPaper);
    }

    /**
     * 创建试卷 + 选题组卷（一次性完成）
     */
    @Transactional
    public ExamPaper createExamPaperWithQuestions(CreateExamPaperRequest req, Long createdBy) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy(教师ID)不能为空");
        }
        if (req.getQuestionIds() == null || req.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择1道题目");
        }

        // 1) 创建试卷基础信息
        ExamPaper paper = new ExamPaper();
        paper.setName(req.getName().trim());
        paper.setDescription(req.getDescription());
        paper.setCreatedBy(createdBy);

        // ✅ 新增：保存试卷默认时长（分钟）
        // 前端不传则给一个兜底 60
        Integer duration = req.getDurationMinutes();
        if (duration == null || duration <= 0) {
            duration = 60;
        }
        paper.setDurationMinutes(duration);

        // totalScore 由题目分值合计得到（避免前端传错）
        paper.setTotalScore(0);
        ExamPaper savedPaper = examPaperRepository.save(paper);

        // 2) 查询题目并创建关联记录
        List<Question> questions = questionRepository.findAllById(req.getQuestionIds());
        if (questions.size() != req.getQuestionIds().size()) {
            throw new IllegalArgumentException("题目ID列表包含不存在的题目，请刷新后重试");
        }

        // 按前端传入顺序组卷：orderIndex 从 1 开始
        int total = 0;
        List<ExamQuestion> eqList = new ArrayList<>();
        for (int i = 0; i < req.getQuestionIds().size(); i++) {
            Long qId = req.getQuestionIds().get(i);
            Question q = questions.stream().filter(x -> x.getId().equals(qId)).findFirst().orElse(null);
            if (q == null) {
                throw new IllegalArgumentException("题目不存在：" + qId);
            }

            ExamQuestion eq = new ExamQuestion();
            eq.setExamPaperId(savedPaper.getId());
            eq.setQuestionId(qId);
            eq.setOrderIndex(i + 1);
            // 默认使用题库题目的分值作为该题在试卷中的分值
            eq.setScore(q.getScore());
            total += (q.getScore() == null ? 0 : q.getScore());
            eqList.add(eq);
        }

        examQuestionRepository.saveAll(eqList);

        // 3) 回写总分
        savedPaper.setTotalScore(total);
        return examPaperRepository.save(savedPaper);
    }

    /**
     * 查询试卷列表，可按创建教师筛选
     */
    public List<ExamPaper> listExamPapers(Long createdBy) {
        if (createdBy != null) {
            return examPaperRepository.findByCreatedBy(createdBy);
        }
        return examPaperRepository.findAll();
    }

    /**
     * 获取试卷详情
     */
    public ExamPaper getExamPaper(Long id) {
        return examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
    }

    /**
     * 更新试卷
     */
    public ExamPaper updateExamPaper(Long id, ExamPaper payload) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
        examPaper.setName(payload.getName());
        examPaper.setDescription(payload.getDescription());
        examPaper.setTotalScore(payload.getTotalScore());

        // ✅ 新增：更新试卷默认时长
        if (payload.getDurationMinutes() != null && payload.getDurationMinutes() > 0) {
            examPaper.setDurationMinutes(payload.getDurationMinutes());
        }

        return examPaperRepository.save(examPaper);
    }

    /**
     * 更新试卷 + 重新选题组卷
     */
    @Transactional
    public ExamPaper updateExamPaperWithQuestions(Long examPaperId, UpdateExamPaperRequest req, Long operatorTeacherId) {
        if (examPaperId == null) {
            throw new IllegalArgumentException("examPaperId 不能为空");
        }
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (operatorTeacherId == null) {
            throw new IllegalArgumentException("教师未登录");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        if (req.getQuestionIds() == null || req.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择1道题目");
        }

        ExamPaper paper = examPaperRepository.findById(examPaperId)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));

        // 简单权限：只有创建者能编辑（你也可以改成管理员逻辑）
        if (paper.getCreatedBy() != null && !paper.getCreatedBy().equals(operatorTeacherId)) {
            throw new IllegalArgumentException("无权限编辑该试卷");
        }

        // 1) 更新试卷基本信息
        paper.setName(req.getName().trim());
        paper.setDescription(req.getDescription());

        // ✅ 新增：如果前端带了时长，就更新
        if (req.getDurationMinutes() != null && req.getDurationMinutes() > 0) {
            paper.setDurationMinutes(req.getDurationMinutes());
        }

        // 2) 清空旧的试卷题目关联
        examQuestionRepository.deleteByExamPaperId(examPaperId);

        // 3) 重建新的试卷题目关联，并计算总分
        List<Question> questions = questionRepository.findAllById(req.getQuestionIds());
        if (questions.size() != req.getQuestionIds().size()) {
            throw new IllegalArgumentException("题目ID列表包含不存在的题目，请刷新后重试");
        }

        int total = 0;
        List<ExamQuestion> eqList = new ArrayList<>();
        for (int i = 0; i < req.getQuestionIds().size(); i++) {
            Long qId = req.getQuestionIds().get(i);
            Question q = questions.stream().filter(x -> x.getId().equals(qId)).findFirst().orElse(null);
            if (q == null) {
                throw new IllegalArgumentException("题目不存在：" + qId);
            }

            ExamQuestion eq = new ExamQuestion();
            eq.setExamPaperId(examPaperId);
            eq.setQuestionId(qId);
            eq.setOrderIndex(i + 1);
            eq.setScore(q.getScore());
            total += (q.getScore() == null ? 0 : q.getScore());
            eqList.add(eq);
        }
        examQuestionRepository.saveAll(eqList);

        paper.setTotalScore(total);
        return examPaperRepository.save(paper);
    }

    /**
     * 删除试卷
     */
    public void deleteExamPaper(Long id) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
        examPaperRepository.delete(examPaper);
    }

    /**
     * 自动随机组卷（按题型分别设置：题数 + 难度（支持每个难度数量））
     *
     * 重要说明：
     * - 这个接口只负责“组卷”，不创建考试场次（exam_sessions）
     * - 组卷规则由老师配置，系统只按规则随机抽题
     * - 若某个规则要求的题数 > 题库可用题数，会直接报错，提醒老师调整规则或先补题
     */
    @Transactional
    public ExamPaper autoAssembleExamPaper(AutoAssembleExamPaperRequest req, Long teacherIdFromSession) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("试卷标题不能为空");
        }
        if (req.getRules() == null || req.getRules().isEmpty()) {
            throw new IllegalArgumentException("请至少配置1条组卷规则");
        }

        Long createdBy = req.getCreatedBy() != null ? req.getCreatedBy() : teacherIdFromSession;
        if (createdBy == null) {
            throw new IllegalArgumentException("教师未登录");
        }

        boolean onlyMine = Boolean.TRUE.equals(req.getOnlyMine());
        boolean shuffleOrder = Boolean.TRUE.equals(req.getShuffleOrder());

        // 支持的题型（与你 QuestionService 一致）
        Set<String> supportedTypes = new HashSet<>(Arrays.asList(
                "single_choice",
                "multiple_choice",
                "true_false",
                "subjective"
        ));
        Set<String> supportedDifficulty = new HashSet<>(Arrays.asList("简单", "中等", "困难"));

        // 最终抽中的题目ID（保持顺序，避免老师预期混乱；如需打乱可选 shuffleOrder）
        List<Long> pickedQuestionIds = new ArrayList<>();
        Set<Long> pickedSet = new HashSet<>();
        Random random = new Random();

        for (AutoAssembleRuleItem rule : req.getRules()) {
            if (rule == null) continue;
            String type = rule.getType() == null ? null : rule.getType().trim();
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("规则中的题型不能为空");
            }
            if (!supportedTypes.contains(type)) {
                throw new IllegalArgumentException("不支持的题型：" + type);
            }
            if (rule.getCounts() == null || rule.getCounts().isEmpty()) {
                throw new IllegalArgumentException("请为题型 " + type + " 配置难度题数");
            }

            // 逐难度抽题，例如：简单2，中等2，困难1
            for (Map.Entry<String, Integer> entry : rule.getCounts().entrySet()) {
                String difficulty = entry.getKey();
                Integer count = entry.getValue();

                if (difficulty == null) continue;
                difficulty = difficulty.trim();
                if (!supportedDifficulty.contains(difficulty)) {
                    throw new IllegalArgumentException("不支持的难度：" + difficulty + "（仅支持：简单/中等/困难）");
                }
                if (count == null || count < 0) {
                    throw new IllegalArgumentException("题数必须是>=0 的整数（type=" + type + ", difficulty=" + difficulty + ")");
                }
                if (count == 0) {
                    continue;
                }

                // 1) 取候选题（注意：这里只取该 type+difficulty 的题；若 onlyMine=true 则限制 createdBy）
                List<Question> candidates = onlyMine
                        ? questionRepository.findByCreatedByAndTypeAndDifficulty(createdBy, type, difficulty)
                        : questionRepository.findByTypeAndDifficulty(type, difficulty);

                if (candidates == null) candidates = Collections.emptyList();

                // 2) 过滤掉已经抽到的题（跨规则去重）
                List<Question> available = new ArrayList<>();
                for (Question q : candidates) {
                    if (q != null && q.getId() != null && !pickedSet.contains(q.getId())) {
                        available.add(q);
                    }
                }

                if (available.size() < count) {
                    throw new IllegalArgumentException(
                            "题库题量不足：题型=" + type + "，难度=" + difficulty +
                                    "，需要" + count + "题，但可用仅" + available.size() + "题。\n" +
                                    "解决建议：减少该规则数量，或先在题库里补充该难度题目。"
                    );
                }

                // 3) 随机抽取 count 道（不重复）
                Collections.shuffle(available, random);
                for (int i = 0; i < count; i++) {
                    Long qId = available.get(i).getId();
                    pickedQuestionIds.add(qId);
                    pickedSet.add(qId);
                }
            }
        }

        if (pickedQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("组卷结果为空：请检查规则题数是否都为0");
        }

        // 可选：打乱最终顺序（有些老师希望同类型题不扎堆）
        if (shuffleOrder) {
            Collections.shuffle(pickedQuestionIds, random);
        }

        // 复用现有的“创建试卷+选题组卷”落库逻辑
        CreateExamPaperRequest createReq = new CreateExamPaperRequest();
        createReq.setName(req.getName());
        createReq.setDescription(req.getDescription());
        createReq.setCreatedBy(createdBy);
        createReq.setQuestionIds(pickedQuestionIds);
        //  新增：把时长传下去
        createReq.setDurationMinutes(req.getDurationMinutes());

        return createExamPaperWithQuestions(createReq, createdBy);
    }
}

