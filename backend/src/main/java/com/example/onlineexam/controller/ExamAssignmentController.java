package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.AssignStudentsRequest;
import com.example.onlineexam.entity.ExamAssignment;
import com.example.onlineexam.service.ExamAssignmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试分配接口：教师分配某场考试给指定学生。
 */
@RestController
@RequestMapping("/api/exam-assignments")
public class ExamAssignmentController {

    private final ExamAssignmentService examAssignmentService;

    public ExamAssignmentController(ExamAssignmentService examAssignmentService) {
        this.examAssignmentService = examAssignmentService;
    }

    /**
     * 教师端：批量分配
     */
    @PostMapping
    public ApiResponse assign(@RequestBody AssignStudentsRequest req, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以分配考试");
        }
        Long teacherId = (Long) session.getAttribute("userId");

        List<ExamAssignment> saved = examAssignmentService.assignStudents(
                req.getExamSessionId(),
                req.getStudentIds(),
                teacherId
        );
        return ApiResponse.success("分配成功", saved);
    }

    /**
     * 教师端：撤销分配
     */
    @DeleteMapping
    public ApiResponse revoke(@RequestParam Long examSessionId, @RequestParam Long studentId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以撤销分配");
        }
        examAssignmentService.revoke(examSessionId, studentId);
        return ApiResponse.success("撤销成功", null);
    }

    /**
     * 教师端：查看某场考试分配给了哪些学生
     */
    @GetMapping
    public ApiResponse listBySession(@RequestParam Long examSessionId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以查看分配名单");
        }
        return ApiResponse.success("查询成功", examAssignmentService.listByExamSession(examSessionId));
    }

    /**
     * 学生端：查看“我被分配的考试”
     */
    @GetMapping("/my")
    public ApiResponse my(@RequestParam Long studentId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"STUDENT".equals(role)) {
            return ApiResponse.error("只有学生可以查询自己的分配");
        }

        // 安全兜底：如果 session 里有 userId，则优先使用，避免前端伪造 studentId
        Long sessionUserId = (Long) session.getAttribute("userId");
        Long realStudentId = sessionUserId != null ? sessionUserId : studentId;

        return ApiResponse.success("查询成功", examAssignmentService.listByStudent(realStudentId));
    }

    /**
     * 教师端：查询某场考试已分配人数
     */
    @GetMapping("/count")
    public ApiResponse count(@RequestParam Long examSessionId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以查看分配人数");
        }
        long cnt = examAssignmentService.countByExamSession(examSessionId);
        return ApiResponse.success("查询成功", cnt);
    }

    /**
     * 教师端：一键撤销/清空某场考试的全部分配
     */
    @DeleteMapping("/clear")
    public ApiResponse clear(@RequestParam Long examSessionId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return ApiResponse.error("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            return ApiResponse.error("只有教师可以清空分配");
        }
        examAssignmentService.clearByExamSession(examSessionId);
        return ApiResponse.success("清空成功", null);
    }
}
