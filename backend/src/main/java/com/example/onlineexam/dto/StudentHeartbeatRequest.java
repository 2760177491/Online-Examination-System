package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 学生端心跳上报请求
 */
@Data
public class StudentHeartbeatRequest {
    private Long examSessionId;
    private Long studentId;
}

