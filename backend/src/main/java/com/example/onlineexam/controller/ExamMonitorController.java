package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.ScreenSwitchRequest;
import com.example.onlineexam.dto.StudentHeartbeatRequest;
import com.example.onlineexam.service.ExamMonitorService;
import org.springframework.web.bind.annotation.*;

/**
 * 第11周：考试监控（简化版）
 */
@RestController
@RequestMapping("/api/monitor")
public class ExamMonitorController {

    private final ExamMonitorService examMonitorService;

    public ExamMonitorController(ExamMonitorService examMonitorService) {
        this.examMonitorService = examMonitorService;
    }

    /**
     * 学生端：心跳上报
     * - 前端可每 15~30 秒上报一次
     */
    @PostMapping("/heartbeat")
    public ApiResponse heartbeat(@RequestBody StudentHeartbeatRequest req) {
        examMonitorService.heartbeat(req.getExamSessionId(), req.getStudentId());
        return ApiResponse.success("心跳上报成功", null);
    }

    /**
     * 学生端：切屏上报
     */
    @PostMapping("/screen-switch")
    public ApiResponse screenSwitch(@RequestBody ScreenSwitchRequest req) {
        examMonitorService.screenSwitch(req.getExamSessionId(), req.getStudentId());
        return ApiResponse.success("切屏记录成功", null);
    }

    /**
     * 教师端：查看某场考试的在线/切屏统计
     */
    @GetMapping("/teacher")
    public ApiResponse teacherList(@RequestParam Long examSessionId) {
        return ApiResponse.success("查询监控列表成功", examMonitorService.listByExamSessionId(examSessionId));
    }
}

