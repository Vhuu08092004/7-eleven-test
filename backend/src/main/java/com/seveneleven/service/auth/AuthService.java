package com.seveneleven.service.auth;

import com.seveneleven.dto.auth.*;
import com.seveneleven.entity.User;
import com.seveneleven.exception.BadRequestException;
import com.seveneleven.exception.UnauthorizedException;
import com.seveneleven.repository.RefreshTokenRepository;
import com.seveneleven.repository.UserRepository;
import com.seveneleven.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = UUID.randomUUID().toString();

        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenRepository.save(refreshToken, user.getId(), 7);

        log.info("User logged in successfully: {}", request.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String oldToken = request.getRefreshToken();

        if (!refreshTokenRepository.exists(oldToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = refreshTokenRepository.findUserIdByToken(oldToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenRepository.delete(oldToken);

        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = UUID.randomUUID().toString();

        refreshTokenRepository.save(newRefreshToken, user.getId(), 7);

        log.info("Token refreshed for user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return UserResponse.fromEntity(user);
    }
}
