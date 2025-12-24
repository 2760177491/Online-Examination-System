package com.example.onlineexam.dto;

import lombok.Data;

/**
 * 学生端切屏上报请求
 */
@Data
public class ScreenSwitchRequest {
    private Long examSessionId;
    private Long studentId;
}

