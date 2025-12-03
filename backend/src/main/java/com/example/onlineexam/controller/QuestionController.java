package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.entity.Question;
import com.example.onlineexam.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库接口
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 创建题目
     */
    @PostMapping
    public ApiResponse create(@RequestBody Question question) {
        Question saved = questionService.createQuestion(question);
        return ApiResponse.success("创建题目成功", saved);
    }

    /**
     * 查询题目列表，可按出题教师或题型筛选
     */
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long createdBy,
                            @RequestParam(required = false) String type) {
        List<Question> questions = questionService.listQuestions(createdBy, type);
        return ApiResponse.success("查询题目列表成功", questions);
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Question question) {
        Question updated = questionService.updateQuestion(id, question);
        return ApiResponse.success("更新题目成功", updated);
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ApiResponse.success("删除题目成功", null);
    }
}