package com.example.onlineexam.service.impl;

import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.entity.StudentAnswer;
import com.example.onlineexam.repository.StudentExamRepository;
import com.example.onlineexam.repository.StudentAnswerRepository;
import com.example.onlineexam.repository.ExamSessionRepository;
import com.example.onlineexam.service.StudentExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生考试服务实现类
 */
@Service
public class StudentExamServiceImpl implements StudentExamService {
    
    @Autowired
    private StudentExamRepository studentExamRepository;
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Autowired
    private ExamSessionRepository examSessionRepository;
    
    @Override
    public StudentExam startExam(Long studentId, Long examSessionId) {
        // 检查考试场次是否存在
        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        
        // 检查考试是否已开始
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(examSession.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        
        // 检查考试是否已结束
        if (now.isAfter(examSession.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }
        
        // 检查学生是否已参加该考试
        StudentExam existingExam = studentExamRepository.findByStudentIdAndExamSessionId(studentId, examSessionId);
        if (existingExam != null) {
            return existingExam;
        }
        
        // 创建新的学生考试记录
        StudentExam studentExam = new StudentExam();
        studentExam.setStudentId(studentId);
        studentExam.setExamSessionId(examSessionId);
        studentExam.setActualStartTime(now);
        studentExam.setStatus("进行中");
        studentExam.setCreatedAt(LocalDateTime.now());
        
        return studentExamRepository.save(studentExam);
    }
    
    @Override
    public StudentExam submitExam(Long studentExamId, List<StudentAnswer> answers) {
        // 获取学生考试记录
        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));
        
        // 检查考试状态，避免重复提交
        if ("已提交".equals(studentExam.getStatus()) || "已批改".equals(studentExam.getStatus())) {
            throw new RuntimeException("考试已提交，无法重复提交");
        }
        
        // 先删除该学生考试下已有的答题记录，避免重复保存
        List<StudentAnswer> existingAnswers = studentAnswerRepository.findByStudentExamId(studentExamId);
        if (!existingAnswers.isEmpty()) {
            studentAnswerRepository.deleteAll(existingAnswers);
        }

        // 保存学生答案（此时仅保存原始答案内容，评分稍后由自动判分服务处理）
        for (StudentAnswer answer : answers) {
            // 强制设置为当前学生考试ID，防止前端篡改
            answer.setStudentExamId(studentExamId);
            // 初始化为“未批改”状态
            answer.setGradingStatus("未批改");
            // 清空得分，避免前端传入伪造分数
            answer.setScore(null);
            studentAnswerRepository.save(answer);
        }
        
        // 更新学生考试记录为“已提交”状态，记录提交时间
        studentExam.setSubmitTime(LocalDateTime.now());
        studentExam.setStatus("已提交");
        
        return studentExamRepository.save(studentExam);
    }
    
    @Override
    public StudentExam getStudentExamById(Long id) {
        return studentExamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生考试记录不存在"));
    }
    
    @Override
    public List<StudentExam> getStudentExamsByStudentId(Long studentId) {
        return studentExamRepository.findByStudentId(studentId);
    }
    
    @Override
    public List<StudentExam> getStudentExamsByExamSessionId(Long examSessionId) {
        return studentExamRepository.findByExamSessionId(examSessionId);
    }
    
    @Override
    public StudentAnswer saveStudentAnswer(StudentAnswer answer) {
        answer.setGradingStatus("未批改");
        return studentAnswerRepository.save(answer);
    }
    
    @Override
    public List<StudentAnswer> getStudentAnswersByStudentExamId(Long studentExamId) {
        return studentAnswerRepository.findByStudentExamId(studentExamId);
    }
}