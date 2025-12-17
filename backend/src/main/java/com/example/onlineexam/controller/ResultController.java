package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.service.StudentExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 成绩接口
 */
@RestController
@RequestMapping("/api/results")
public class ResultController {

    @Autowired
    private StudentExamService studentExamService;

    /**
     * 验证用户身份
     */
    private Long getStudentId() {
        // 从请求头或安全上下文中获取学生ID
        // 这里简化处理，直接返回1作为示例
        return 1L;
    }

    /**
     * 获取成绩列表
     */
    @GetMapping
    public ApiResponse list() {
        // 暂时返回空列表，后续实现具体功能
        return ApiResponse.success("查询成绩列表成功", new ArrayList<>());
    }

    /**
     * 获取我的成绩列表
     */
    @GetMapping("/my")
    public ApiResponse myResults(@RequestParam Long studentId) {
        // 根据前端传入的 studentId 查询该学生的考试记录，前端可根据需要展示为“我的成绩”
        List<?> results = studentExamService.getStudentExamsByStudentId(studentId);
        return ApiResponse.success("查询我的成绩列表成功", results);
    }

    /**
     * 获取成绩详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        // 暂时返回null，后续实现具体功能
        return ApiResponse.success("获取成绩详情成功", null);
    }
}