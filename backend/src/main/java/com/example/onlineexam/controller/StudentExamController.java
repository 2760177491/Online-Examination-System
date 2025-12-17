package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.GradingResultDto;
import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.entity.StudentAnswer;
import com.example.onlineexam.service.GradingService;
import com.example.onlineexam.service.StudentExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生考试接口
 */
@RestController
@RequestMapping("/api/student-exams")
public class StudentExamController {
    
    private final StudentExamService studentExamService;
    private final GradingService gradingService;

    public StudentExamController(StudentExamService studentExamService,
                                 GradingService gradingService) {
        this.studentExamService = studentExamService;
        this.gradingService = gradingService;
    }
    
    /**
     * 学生开始考试
     */
    @PostMapping("/start")
    public ApiResponse start(@RequestParam Long studentId, @RequestParam Long examSessionId) {
        StudentExam studentExam = studentExamService.startExam(studentId, examSessionId);
        return ApiResponse.success("开始考试成功", studentExam);
    }
    
    /**
     * 学生提交考试
     * 前端先提交所有答案到后端保存，然后立即进行自动判分，
     * 返回总分和每题得分信息，方便前端提示“提交成功，得分为 X 分”。
     */
    @PostMapping("/submit")
    public ApiResponse submit(@RequestParam Long studentExamId, @RequestBody List<StudentAnswer> answers) {
        // 1. 保存学生答案并更新考试状态为“已提交”
        StudentExam studentExam = studentExamService.submitExam(studentExamId, answers);

        // 2. 调用自动判分服务，对本次考试进行客观题判分，计算总分
        GradingResultDto gradingResult = gradingService.autoGrade(studentExam.getId());

        // 3. 返回判分结果给前端
        return ApiResponse.success("提交考试成功", gradingResult);
    }
    
    /**
     * 获取学生考试详情
     */
    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        StudentExam studentExam = studentExamService.getStudentExamById(id);
        return ApiResponse.success("获取学生考试详情成功", studentExam);
    }
    
    /**
     * 获取学生考试列表
     */
    @GetMapping
    public ApiResponse list(@RequestParam Long studentId) {
        List<StudentExam> studentExams = studentExamService.getStudentExamsByStudentId(studentId);
        return ApiResponse.success("获取学生考试列表成功", studentExams);
    }
    
    /**
     * 获取学生答案列表
     */
    @GetMapping("/{id}/answers")
    public ApiResponse getAnswers(@PathVariable Long id) {
        List<StudentAnswer> answers = studentExamService.getStudentAnswersByStudentExamId(id);
        return ApiResponse.success("获取学生答案列表成功", answers);
    }
}