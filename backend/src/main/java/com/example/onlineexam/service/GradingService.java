package com.example.onlineexam.service;

import com.example.onlineexam.dto.GradingResultDto;

/**
 * 自动判分服务接口
 */
public interface GradingService {

    /**
     * 对指定学生考试记录进行自动判分（客观题）
     *
     * @param studentExamId 学生考试ID
     * @return 判分结果（包含总分和每题得分信息）
     */
    GradingResultDto autoGrade(Long studentExamId);
}

