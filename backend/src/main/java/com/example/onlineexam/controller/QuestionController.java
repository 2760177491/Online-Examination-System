package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.entity.Question;
import com.example.onlineexam.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

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
    public ApiResponse create(@RequestBody Question question, HttpSession session) {
        // ============================
        // 关键改动：从登录会话兜底 createdBy
        // ============================
        // 说明：
        // 1) 前端现在会自动带上 createdBy（teacherId），但为了防止“未登录直接访问/本地存储丢失”等情况，后端再兜底一次。
        // 2) 同时做最基本的权限校验：只有 TEACHER 允许出题。
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能创建题目");
        }

        Long userId = (Long) session.getAttribute("userId");
        if (question.getCreatedBy() == null) {
            question.setCreatedBy(userId);
        }

        Question saved = questionService.createQuestion(question);
        return ApiResponse.success("创建题目成功", saved);
    }

    /**
     * 查询题目列表，支持组合筛选：
     * - createdBy：教师ID
     * - type：题型
     * - keyword：题干关键字（包含匹配，忽略大小写）
     */
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long createdBy,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String difficulty) {
        List<Question> questions = questionService.listQuestions(createdBy, type, keyword, difficulty);
        return ApiResponse.success("查询题目列表成功", questions);
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Question question, HttpSession session) {
        // ============================
        // 关键改动：限制只有教师能改题，并兜底 createdBy
        // ============================
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能修改题目");
        }

        Long userId = (Long) session.getAttribute("userId");
        if (question.getCreatedBy() == null) {
            question.setCreatedBy(userId);
        }

        Question updated = questionService.updateQuestion(id, question);
        return ApiResponse.success("更新题目成功", updated);
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师才能删除题目");
        }

        questionService.deleteQuestion(id);
        return ApiResponse.success("删除题目成功", null);
    }
}