package com.example.whisperQ.global.init;

import com.example.whisperQ.domain.auth.entity.User;
import com.example.whisperQ.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 관리자 계정이 존재하는지 확인
        if (!userRepository.existsByEmail("admin@whisperq.com")) {
            User admin = User.builder()
                    .email("admin@whisperq.com") 
                    .password(passwordEncoder.encode("admin@whisperq.com")) // 비밀번호 암호화
                    .build();
            userRepository.save(admin);
            System.out.println("Initialized default admin user: admin@whisperq.com / admin@whisperq.com");
        }
    }
}
