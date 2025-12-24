package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.TeacherGradeSubjectiveRequest;
import com.example.onlineexam.service.TeacherSubjectiveGradingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

/**
 * 教师端：主观题批改接口
 */
@RestController
@RequestMapping("/api/teacher-grading")
public class TeacherSubjectiveGradingController {

    private final TeacherSubjectiveGradingService service;

    public TeacherSubjectiveGradingController(TeacherSubjectiveGradingService service) {
        this.service = service;
    }

    /**
     * 待批改列表：某场考试中，哪些学生的主观题还没批改
     */
    @GetMapping("/pending")
    public ApiResponse pending(@RequestParam Long examSessionId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以批改");
        }
        Long teacherId = (Long) session.getAttribute("userId");
        return ApiResponse.success("查询成功", service.listPending(examSessionId, teacherId));
    }

    /**
     * 批改页面：查看某个学生考试的所有主观题
     */
    @GetMapping("/detail")
    public ApiResponse detail(@RequestParam Long studentExamId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以批改");
        }
        Long teacherId = (Long) session.getAttribute("userId");
        return ApiResponse.success("查询成功", service.getSubjectiveAnswers(studentExamId, teacherId));
    }

    /**
     * 批量打分
     */
    @PostMapping("/grade")
    public ApiResponse grade(@RequestBody TeacherGradeSubjectiveRequest req, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以批改");
        }
        Long teacherId = (Long) session.getAttribute("userId");
        return ApiResponse.success("批改成功", service.grade(req, teacherId));
    }
}

