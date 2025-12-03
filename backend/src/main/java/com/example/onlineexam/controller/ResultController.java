package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 成绩接口
 */
@RestController
@RequestMapping("/api/results")
public class ResultController {

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
    public ApiResponse myResults() {
        // 暂时返回空列表，后续实现具体功能
        return ApiResponse.success("查询我的成绩列表成功", new ArrayList<>());
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