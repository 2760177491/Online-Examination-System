package com.example.onlineexam.service;

import com.example.onlineexam.entity.Question;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 题库业务逻辑
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    public List<Question> listQuestions(Long createdBy, String type) {
        if (createdBy != null) {
            return questionRepository.findByCreatedBy(createdBy);
        }
        if (type != null) {
            return questionRepository.findByType(type);
        }
        return questionRepository.findAll();
    }

    public Question updateQuestion(Long id, Question payload) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));
        question.setTitle(payload.getTitle());
        question.setType(payload.getType());
        question.setOptionsJson(payload.getOptionsJson());
        question.setCorrectAnswer(payload.getCorrectAnswer());
        question.setScore(payload.getScore());
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}