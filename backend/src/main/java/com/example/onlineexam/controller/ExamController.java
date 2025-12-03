package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 试卷接口
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * 创建试卷
     */
    @PostMapping
    public ApiResponse create(@RequestBody ExamPaper examPaper) {
        ExamPaper saved = examService.createExamPaper(examPaper);
        return ApiResponse.success("创建试卷成功", saved);
    }

    /**
     * 查询试卷列表，可按创建教师筛选
     */
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long createdBy) {
        List<ExamPaper> examPapers = examService.listExamPapers(createdBy);
        return ApiResponse.success("查询试卷列表成功", examPapers);
    }

    /**
     * 获取试卷详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        ExamPaper examPaper = examService.getExamPaper(id);
        return ApiResponse.success("获取试卷详情成功", examPaper);
    }

    /**
     * 更新试卷
     */
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody ExamPaper examPaper) {
        ExamPaper updated = examService.updateExamPaper(id, examPaper);
        return ApiResponse.success("更新试卷成功", updated);
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        examService.deleteExamPaper(id);
        return ApiResponse.success("删除试卷成功", null);
    }
    
    /**
     * 获取学生可参加的考试列表
     */
    @GetMapping("/available")
    public ApiResponse availableExams() {
        // 暂时返回空列表，后续实现具体功能
        return ApiResponse.success("查询可参加考试列表成功", new ArrayList<>());
    }
    
    /**
     * 获取学生已参加的考试列表
     */
    @GetMapping("/my")
    public ApiResponse myExams() {
        // 暂时返回空列表，后续实现具体功能
        return ApiResponse.success("查询我的考试列表成功", new ArrayList<>());
    }
}