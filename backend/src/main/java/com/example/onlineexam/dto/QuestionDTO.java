package com.onlineexam.dto;

import lombok.Data;
import java.util.Map;

@Data
public class QuestionDTO {
    private Long id;
    private String type;
    private String content;
    private Map<String, String> options;
    private String answer;
    private Integer score;
    private Integer sortOrder;
}