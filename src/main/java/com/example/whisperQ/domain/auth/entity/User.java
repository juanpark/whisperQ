package com.example.whisperQ.domain.auth.entity;

import com.example.whisperQ.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 진행자(Facilitator) 정보
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
}
