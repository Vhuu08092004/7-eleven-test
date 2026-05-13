package com.seveneleven.service.auth;

import com.seveneleven.dto.auth.LoginRequest;
import com.seveneleven.dto.auth.LoginResponse;
import com.seveneleven.dto.auth.RefreshTokenRequest;
import com.seveneleven.entity.User;
import com.seveneleven.exception.UnauthorizedException;
import com.seveneleven.repository.RefreshTokenRepository;
import com.seveneleven.repository.UserRepository;
import com.seveneleven.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@7eleven.com")
                .password("encoded_password")
                .role(User.Role.ROLE_USER)
                .isDeleted(false)
                .build();
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("user@7eleven.com", "password");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access_token");
        when(userRepository.findByEmailAndIsDeletedFalse("user@7eleven.com"))
                .thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).save(any(), any(), anyLong());

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(authenticationManager).authenticate(any());
        verify(refreshTokenRepository).save(any(), eq(1L), eq(7L));
    }

    @Test
    void login_UserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@7eleven.com", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_Success() {
        String oldToken = "old_refresh_token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldToken);

        when(refreshTokenRepository.exists(oldToken)).thenReturn(true);
        when(refreshTokenRepository.findUserIdByToken(oldToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUser.getEmail())).thenReturn("new_access_token");
        doNothing().when(refreshTokenRepository).delete(oldToken);
        doNothing().when(refreshTokenRepository).save(any(), any(), anyLong());

        LoginResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).save(any(), eq(1L), eq(7L));
    }

    @Test
    void refreshToken_InvalidToken() {
        String invalidToken = "invalid_token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

        when(refreshTokenRepository.exists(invalidToken)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }
}
