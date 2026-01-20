package com.example.whisperQ.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
    String sessionId,
    
    // 로그인한 경우에만 보냄 (없으면 null)
    Long userId, 
    
    // (선택) 클라이언트가 생성해서 보낸 임시 닉네임이 있다면 사용, 없으면 서버가 생성
    String guestName, 

    @NotBlank(message = "질문 내용을 입력해주세요.")
    @Size(max = 200, message = "질문은 200자 이내로 입력해주세요.")
    String text
) {}