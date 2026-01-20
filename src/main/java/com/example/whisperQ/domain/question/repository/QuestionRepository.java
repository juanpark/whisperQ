package com.example.whisperQ.domain.question.repository;

import com.example.whisperQ.domain.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
}
