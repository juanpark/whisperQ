package com.example.whisperQ.domain.reaction.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.whisperQ.domain.reaction.dto.ReactionMessage;
import com.example.whisperQ.domain.reaction.dto.ReactionUpdateEvent;
import com.example.whisperQ.domain.reaction.entity.ReactionLog;
import com.example.whisperQ.domain.reaction.entity.ReactionType;
import com.example.whisperQ.domain.reaction.repository.ReactionLogRepository;
import com.example.whisperQ.domain.session.entity.Session;
import com.example.whisperQ.domain.session.repository.SessionRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ReactionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReactionLogRepository reactionLogRepository;
    private final SessionRepository sessionRepository;

    // Window size (30 seconds)
    private static final long TIME_WINDOW_MS = 30000;

    /**
     * Save a reaction to Redis using ZSET.
     * key: reaction:session:{sessionId}:{type}
     * score: timestamp
     * member: uniqueId (timestamp + random)
     * 
     * Also tracks user-specific reaction rate.
     */
    @Transactional
    public void saveReaction(ReactionMessage message, String userId) {
        String sessionIdStr = message.sessionId();
        String typeStr = message.type();
        long timestamp = System.currentTimeMillis();
        
        // DB 저장 로직 추가
        Session session = sessionRepository.findBySessionCode(sessionIdStr)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with code: " + sessionIdStr));
        
        ReactionLog log = ReactionLog.builder()
                .session(session)
                .type(ReactionType.valueOf(typeStr.toUpperCase()))
                .build();
        
        reactionLogRepository.save(log);

        // Redis Key 설계
        // 1. 단순 누적 카운트: session:{id}:reaction:{type}:count
        String countKey = "session:" + sessionIdStr + ":reaction:" + typeStr + ":count";
        redisTemplate.opsForValue().increment(countKey);

        // 2. 최근 30초 윈도우 (ZSet): session:{id}:reaction:{type}:window
        String windowKey = "session:" + sessionIdStr + ":reaction:" + typeStr + ":window";
        
        // Member UniqueID (UUID)
        String member = UUID.randomUUID().toString();

        // ZSet에 저장 (Score: Timestamp)
        redisTemplate.opsForZSet().add(windowKey, member, timestamp);
        
        // 자동 만료 설정 (윈도우 크기보다 조금 넉넉하게)
        redisTemplate.expire(windowKey, Duration.ofMillis(TIME_WINDOW_MS + 10000));
        redisTemplate.expire(countKey, Duration.ofHours(2)); // 누적 카운트는 오래 유지
        
        // 활성 세션 추적
        redisTemplate.opsForSet().add("active_sessions", sessionIdStr);
        redisTemplate.expire("active_sessions", Duration.ofMinutes(5));

        // 3. 사용자별 리액션 빈도 체크 (5초 내 5회)
        if (userId != null) {
            checkUserUrgency(sessionIdStr, userId, timestamp);
        }
    }

    private void checkUserUrgency(String sessionId, String userId, long timestamp) {
        // 사용자별 최근 반응 기록 (타입 구분 없이 통합 카운트 여부는 기획에 따라 다름. 여기서는 통합 카운트로 가정하거나 타입별로 할 수 있음.
        // 요구사항: "누가 어떤 버튼(confused, more)을 5초에 5번 이상 누르면" -> 버튼 구분 없이 누르는 행위 자체로 급박함 판단.
        // 또는 버튼별로 판단? "어떤 버튼을... 5번 이상" -> 버튼별로 판단하는게 원문에 가까움.
        // 하지만 급박함(Urgent)의 정의가 "반응을 많이 하는 것"이라면 통합이 나을 수 있음.
        // 여기서는 "어떤 버튼(confused, more)" 이라고 명시되었으므로, 해당 타입에 대한 집중 클릭을 감지하는 것이 더 정확할 수 있으나,
        // 사용자 상태(Urgent)는 세션 내에서 전역적일 가능성이 높음. 단순화를 위해 "세션 내 반응 총 횟수"로 접근하거나, "타입별 횟수"로 접근.
        // "누가 어떤 버튼...을" -> 특정 버튼을 5번.
        
        // 구현: 사용자별-타입별 윈도우보다는, 사용자별 전체 반응 윈도우가 '급박함'을 더 잘 나타낼 수 있음.
        // 여기서는 "사용자별 통합 반응 윈도우"를 사용하겠습니다. (어떤 버튼이든 막 누르면 급박함)
        String userWindowKey = "session:" + sessionId + ":user:" + userId + ":window";
        
        redisTemplate.opsForZSet().add(userWindowKey, UUID.randomUUID().toString(), timestamp);
        redisTemplate.expire(userWindowKey, Duration.ofSeconds(10));

        long windowStart = timestamp - 5000; // 5초 전
        redisTemplate.opsForZSet().removeRangeByScore(userWindowKey, 0, windowStart - 1);
        
        Long count = redisTemplate.opsForZSet().zCard(userWindowKey);
        
        if (count != null && count >= 5) {
            // 급박한 사용자로 등록
            String urgentUsersKey = "session:" + sessionId + ":urgent_users";
            redisTemplate.opsForSet().add(urgentUsersKey, userId);
            redisTemplate.expire(urgentUsersKey, Duration.ofMinutes(1)); // 1분간 유지 (혹은 적절한 만료 시간)
        }
    }

    /**
     * 최근 30초간의 반응 개수를 계산함.
     */
    @Transactional
    public ReactionUpdateEvent calculateIntensity(String sessionId) {
        long now = System.currentTimeMillis();
        long windowStart = now - TIME_WINDOW_MS;

        Map<String, Long> counts = new HashMap<>();
        
        // Iterate over all reaction types
        for (ReactionType type : ReactionType.values()) {
            String typeName = type.name();
            String windowKey = "session:" + sessionId + ":reaction:" + typeName + ":window";

            // 1. 30초 지난 데이터 삭제 (ZREMRANGEBYSCORE)
            redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart - 1);

            // 2. 현재 윈도우 내 개수 조회 (ZCARD)
            // 이미 삭제했으므로 전체 개수 = 윈도우 내 개수
            Long count = redisTemplate.opsForZSet().zCard(windowKey);
            
            if (count != null && count > 0) {
                counts.put(typeName, count);
            }
        }

        // 3. 급박한 사용자 수 조회
        String urgentUsersKey = "session:" + sessionId + ":urgent_users";
        Long urgentUserCount = redisTemplate.opsForSet().size(urgentUsersKey);
        int urgentCountVal = (urgentUserCount != null) ? urgentUserCount.intValue() : 0;

        return new ReactionUpdateEvent(sessionId, counts, urgentCountVal);
    }
}
