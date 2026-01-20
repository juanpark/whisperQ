package com.example.whisperQ.global.util;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {
    private static final List<String> ADJECTIVES = List.of(
        "행복한", "즐거운", "용감한", "수줍은", "똑똑한", 
        "나른한", "배고픈", "신나는", "차분한", "열정적인",
        "엉뚱한", "화려한", "소심한", "친절한", "시크한"
    );

    private static final List<String> NOUNS = List.of(
        "다람쥐", "호랑이", "소나무", "구름", "바다",
        "쿼카", "독수리", "선인장", "우주", "초콜릿",
        "알파카", "고양이", "강아지", "펭귄", "거북이"
    );

    private final SecureRandom random = new SecureRandom();

    public String generate(){
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));

        // 예: 행복한 다람쥐"
        return adjective + noun;
    }
}
