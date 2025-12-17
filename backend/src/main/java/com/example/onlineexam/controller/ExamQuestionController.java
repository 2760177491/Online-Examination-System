package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.ExamQuestionDto;
import com.example.onlineexam.entity.ExamQuestion;
import com.example.onlineexam.entity.Question;
import com.example.onlineexam.repository.ExamQuestionRepository;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 试卷题目关联接口
 *
 * 提供根据试卷ID查询该试卷包含的题目顺序、分值和题干等信息的接口，
 * 主要用于考试作答页面加载题目列表。
 */
@RestController
@RequestMapping("/api/exam-questions")
public class ExamQuestionController {

    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;

    public ExamQuestionController(ExamQuestionRepository examQuestionRepository,
                                  QuestionRepository questionRepository) {
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * 根据试卷ID查询对应的试卷题目列表（按题号升序），并附带题干、题型和选项信息
     *
     * @param examPaperId 试卷ID
     * @return 按 orderIndex 排序的 ExamQuestionDto 列表
     */
    @GetMapping
    public ApiResponse listByExamPaperId(@RequestParam Long examPaperId) {
        // 1. 先查询该试卷下的所有关联题目（只包含 questionId、orderIndex、score 等）
        List<ExamQuestion> examQuestions = examQuestionRepository
                .findByExamPaperIdOrderByOrderIndex(examPaperId);

        if (examQuestions.isEmpty()) {
            return ApiResponse.success("该试卷暂无关联题目", Collections.emptyList());
        }

        // 2. 收集所有 questionId，一次性从题库表中查询，避免 N+1 查询问题
        Set<Long> questionIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toSet());

        List<Question> questions = questionRepository.findAllById(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 3. 将 ExamQuestion 与 Question 组装成前端需要的 DTO
        List<ExamQuestionDto> dtoList = examQuestions.stream().map(eq -> {
            ExamQuestionDto dto = new ExamQuestionDto();
            dto.setId(eq.getId());
            dto.setOrderIndex(eq.getOrderIndex());
            dto.setScore(eq.getScore());
            dto.setQuestionId(eq.getQuestionId());

            Question q = questionMap.get(eq.getQuestionId());
            if (q != null) {
                dto.setContent(q.getTitle());
                dto.setType(q.getType()); // 题型：single_choice / true_false / subjective
                dto.setOptionsJson(q.getOptionsJson());
            }

            return dto;
        }).collect(Collectors.toList());

        return ApiResponse.success("查询试卷题目列表成功", dtoList);
    }
}
