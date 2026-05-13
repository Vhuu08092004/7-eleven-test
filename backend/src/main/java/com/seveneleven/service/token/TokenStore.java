package com.seveneleven.service.token;

import com.seveneleven.entity.RefreshToken;
import com.seveneleven.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStore {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public void save(String token, Long userId, int expirationDays) {
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpirationMs);
        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();
        refreshTokenJpaRepository.save(rt);
        log.debug("Refresh token saved to DB for userId: {}", userId);
    }

    public Optional<Long> findUserIdByToken(String token) {
        return refreshTokenJpaRepository.findByToken(token)
                .filter(RefreshToken::isValid)
                .map(RefreshToken::getUserId);
    }

    public boolean exists(String token) {
        return refreshTokenJpaRepository.findByToken(token)
                .map(RefreshToken::isValid)
                .orElse(false);
    }

    @Transactional
    public void delete(String token) {
        refreshTokenJpaRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenJpaRepository.save(rt);
        });
    }
}
