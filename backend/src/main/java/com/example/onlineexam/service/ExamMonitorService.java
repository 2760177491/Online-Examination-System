package com.example.onlineexam.service;

import com.example.onlineexam.dto.MonitorLogDto;

import java.util.List;

public interface ExamMonitorService {

    /** 学生心跳 */
    void heartbeat(Long examSessionId, Long studentId);

    /** 学生切屏事件 */
    void screenSwitch(Long examSessionId, Long studentId);

    /** 教师端：查看某场考试的监控列表 */
    List<MonitorLogDto> listByExamSessionId(Long examSessionId);
}

