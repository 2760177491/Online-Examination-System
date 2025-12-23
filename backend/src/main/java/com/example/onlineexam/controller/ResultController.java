package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.StudentResultDetailDto;
import com.example.onlineexam.dto.StudentResultListItemDto;
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
     * 获取成绩列表
     */
    @GetMapping
    public ApiResponse list() {
        // 暂时返回空列表，后续实现具体功能
        return ApiResponse.success("查询成绩列表成功", new ArrayList<>());
    }

    /**
     * 获取我的成绩列表（学生端使用）
     */
    @GetMapping("/my")
    public ApiResponse myResults(@RequestParam Long studentId) {
        // 根据前端传入的 studentId 查询该学生的考试记录，前端可根据需要展示为“我的成绩”
        List<StudentResultListItemDto> results = studentExamService.getMyResultList(studentId);
        return ApiResponse.success("查询我的成绩列表成功", results);
    }

    /**
     * 获取成绩详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        return ApiResponse.success("获取成绩详情成功: " + id, null);
    }

    /**
     * 成绩详情（用于“查看详情”按钮）
     */
    @GetMapping("/{studentExamId}/detail")
    public ApiResponse detail(@PathVariable Long studentExamId) {
        StudentResultDetailDto detail = studentExamService.getResultDetail(studentExamId);
        return ApiResponse.success("获取成绩详情成功", detail);
    }
}