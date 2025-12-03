package com.example.onlineexam.service;

import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.repository.ExamPaperRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 试卷业务逻辑
 */
@Service
public class ExamService {

    private final ExamPaperRepository examPaperRepository;

    public ExamService(ExamPaperRepository examPaperRepository) {
        this.examPaperRepository = examPaperRepository;
    }

    /**
     * 创建试卷
     */
    public ExamPaper createExamPaper(ExamPaper examPaper) {
        return examPaperRepository.save(examPaper);
    }

    /**
     * 查询试卷列表，可按创建教师筛选
     */
    public List<ExamPaper> listExamPapers(Long createdBy) {
        if (createdBy != null) {
            return examPaperRepository.findByCreatedBy(createdBy);
        }
        return examPaperRepository.findAll();
    }

    /**
     * 获取试卷详情
     */
    public ExamPaper getExamPaper(Long id) {
        return examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
    }

    /**
     * 更新试卷
     */
    public ExamPaper updateExamPaper(Long id, ExamPaper payload) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
        examPaper.setName(payload.getName());
        examPaper.setDescription(payload.getDescription());
        examPaper.setTotalScore(payload.getTotalScore());
        return examPaperRepository.save(examPaper);
    }

    /**
     * 删除试卷
     */
    public void deleteExamPaper(Long id) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));
        examPaperRepository.delete(examPaper);
    }
}