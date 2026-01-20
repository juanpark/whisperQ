package com.example.whisperQ.domain.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.whisperQ.domain.question.dto.QuestionCreateRequest;
import com.example.whisperQ.domain.question.dto.QuestionResponse;
import com.example.whisperQ.domain.question.service.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class QuestionController {
    
    private final QuestionService questionService;
    
    @PostMapping("/api/questions")
    public ResponseEntity<Long> createQuestion(@RequestBody @Valid QuestionCreateRequest request) {
        Long questionId = questionService.createQuestion(request);
        return ResponseEntity.ok(questionId);
    }

    @GetMapping("/api/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions() {
        List<QuestionResponse> questions = questionService.getQuestions();
        return ResponseEntity.ok(questions);
    }
}
