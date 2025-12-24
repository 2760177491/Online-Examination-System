package com.example.onlineexam.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端监控列表返回的 DTO
 */
@Data
public class MonitorLogDto {
    private Long studentId;
    private String studentUsername;
    private String studentRealName;

    private LocalDateTime lastHeartbeatTime;
    private Integer switchCount;
}

