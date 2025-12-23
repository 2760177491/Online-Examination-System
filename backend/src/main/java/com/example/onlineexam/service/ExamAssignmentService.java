package com.example.onlineexam.service;

import com.example.onlineexam.entity.ExamAssignment;

import java.util.List;

/**
 * 考试分配服务
 */
public interface ExamAssignmentService {

    /** 批量分配：把某场考试分配给多个学生 */
    List<ExamAssignment> assignStudents(Long examSessionId, List<Long> studentIds, Long teacherId);

    /** 撤销分配 */
    void revoke(Long examSessionId, Long studentId);

    /** 查询某场考试的分配名单 */
    List<ExamAssignment> listByExamSession(Long examSessionId);

    /** 查询某学生被分配的考试 */
    List<ExamAssignment> listByStudent(Long studentId);

    /** 是否已分配 */
    boolean isAssigned(Long examSessionId, Long studentId);

    /** 查询某场考试已分配人数 */
    long countByExamSession(Long examSessionId);

    /** 清空某场考试的全部分配（教师一键撤销） */
    void clearByExamSession(Long examSessionId);
}
