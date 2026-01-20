package com.example.whisperQ.domain.question.dto;

import java.time.LocalDateTime;
import com.example.whisperQ.domain.question.entity.Question;

public record QuestionResponse(
    Long id,
    String content,
    LocalDateTime createdAt
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
            question.getId(),
            question.getContent(),
            question.getCreatedAt()
        );
    }
}
