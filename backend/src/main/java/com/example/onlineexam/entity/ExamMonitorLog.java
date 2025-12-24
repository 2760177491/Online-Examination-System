package com.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第11周：考试监控（简化版）
 *
 * 设计目标：
 * - 学生端定时上报心跳（在线/最后上报时间）
 * - 监听切屏事件（visibilitychange）上报，累计切屏次数
 *
 * 注意：这只是“展示/验收用”的简化实现，不涉及摄像头、严谨防作弊。
 */
@Data
@Entity
public class ExamMonitorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 考试场次ID */
    private Long examSessionId;

    /** 学生ID */
    private Long studentId;

    /** 最后一次心跳时间 */
    private LocalDateTime lastHeartbeatTime;

    /** 切屏次数（visibilitychange 触发） */
    private Integer switchCount = 0;

    /** 备注：最后一次事件类型（heartbeat/switch） */
    private String lastEvent;
}
