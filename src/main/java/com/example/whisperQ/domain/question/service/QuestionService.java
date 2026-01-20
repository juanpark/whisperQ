package com.example.whisperQ.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.whisperQ.domain.auth.entity.User;
import com.example.whisperQ.domain.auth.repository.UserRepository;
import com.example.whisperQ.domain.question.dto.QuestionCreateRequest;
import com.example.whisperQ.domain.question.entity.Question;
import com.example.whisperQ.domain.question.repository.QuestionRepository;
import com.example.whisperQ.domain.session.entity.Session;
import com.example.whisperQ.domain.session.repository.SessionRepository;
import com.example.whisperQ.global.util.NicknameGenerator;
import com.example.whisperQ.domain.question.dto.QuestionResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;
    
    @Transactional
    public Long createQuestion(QuestionCreateRequest request) {
        // 1. 세션 조회
        // 프론트엔드에서 세션 코드(String)를 보내므로 바로 조회
        Session session = sessionRepository.findBySessionCode(request.sessionId())
                .orElseThrow(() -> new EntityNotFoundException("Session not found with code: " + request.sessionId()));
        Long sessionId = session.getId();

        // 2. 작성자 이름 결정
        String writerName;
        Long userId = request.userId();

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
            // User 엔티티에 닉네임이 없으므로 이메일 앞부분 등을 사용하거나 기획에 따라 결정
            // 여기서는 이메일 사용
            writerName = user.getEmail(); 
        } else {
            if (request.guestName() != null && !request.guestName().isBlank()) {
                writerName = request.guestName();
            } else {
                writerName = nicknameGenerator.generate();
            }
        }

        // 3. 질문 엔티티 생성 및 저장
        Question question = Question.builder()
                .session(session)
                .content(request.text())
                .writerName(writerName)
                .userId(userId)
                .build();

        return questionRepository.save(question).getId();
    }
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions() {
        return questionRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .map(QuestionResponse::from)
                .collect(Collectors.toList());
    }
}
