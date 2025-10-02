package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.request.RefreshTokenRequest;
import com.chatter.chatter.request.UserLoginRequest;
import com.chatter.chatter.service.AuthenticationService;
import com.chatter.chatter.service.JwtService;
import com.chatter.chatter.service.OnlineUserService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OnlineUserService onlineUserService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private UserLoginRequest userLoginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private TokenDto tokenDto;
    private UserDetails userDetails;
    private User user;
    private final String email = "test@example.com";

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email(email)
                .username("testUser")
                .password("password")
                .build();

        userLoginRequest = new UserLoginRequest(email, "password");
        refreshTokenRequest = new RefreshTokenRequest("refresh-token");
        tokenDto = new TokenDto("access-token", "refresh-token");

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("password")
                .authorities("USER")
                .build();
    }

    @Test
    void shouldLoginSuccessfully() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userLoginRequest.getEmail())).thenReturn(tokenDto);

        TokenDto result = authenticationService.login(userLoginRequest);

        assertNotNull(result);
        assertEquals(tokenDto, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(userLoginRequest.getEmail());
    }

    @Test
    void shouldThrowBadRequest_WhenAuthenticationFails() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        assertThrows(BadRequestException.class, () -> authenticationService.login(userLoginRequest));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken())).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(refreshTokenRequest.getRefreshToken(), userDetails)).thenReturn(true);
        when(jwtService.refreshToken(refreshTokenRequest.getRefreshToken())).thenReturn(tokenDto);

        TokenDto result = authenticationService.refreshToken(refreshTokenRequest);

        assertNotNull(result);
        assertEquals(tokenDto, result);
        verify(jwtService).extractUsername(refreshTokenRequest.getRefreshToken());
        verify(jwtService).isRefreshTokenValid(refreshTokenRequest.getRefreshToken(), userDetails);
        verify(jwtService).refreshToken(refreshTokenRequest.getRefreshToken());
    }

    @Test
    void shouldThrowBadRequest_WhenRefreshTokenInvalid() {
        when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken())).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(refreshTokenRequest.getRefreshToken(), userDetails)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authenticationService.refreshToken(refreshTokenRequest));
        verify(jwtService, never()).refreshToken(anyString());
    }

    @Test
    void shouldThrowBadRequest_WhenRefreshTokenExpired() {
        when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken()))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        assertThrows(BadRequestException.class, () -> authenticationService.refreshToken(refreshTokenRequest));
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).refreshToken(anyString());
    }

    @Test
    void shouldLogoutSuccessfully() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenRequest.getRefreshToken())
                .user(user)
                .build();

        when(refreshTokenRepository.findByTokenAndUserEmail(
                refreshTokenRequest.getRefreshToken(),
                email
        )).thenReturn(Optional.of(refreshToken));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(refreshTokenRequest.getRefreshToken(), userDetails)).thenReturn(true);

        authenticationService.logout(refreshTokenRequest, email);

        verify(onlineUserService).userDisconnected(email);
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void shouldThrowBadRequest_WhenLogoutWithInvalidToken() {
        when(refreshTokenRepository.findByTokenAndUserEmail(
                refreshTokenRequest.getRefreshToken(),
                email
        )).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authenticationService.logout(refreshTokenRequest, email));

        verify(onlineUserService, never()).userDisconnected(anyString());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void shouldThrowBadRequest_WhenLogoutWithInvalidRefreshTokenValidation() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenRequest.getRefreshToken())
                .user(user)
                .build();

        when(refreshTokenRepository.findByTokenAndUserEmail(
                refreshTokenRequest.getRefreshToken(),
                email
        )).thenReturn(Optional.of(refreshToken));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(refreshTokenRequest.getRefreshToken(), userDetails)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> authenticationService.logout(refreshTokenRequest, email));

        verify(onlineUserService, never()).userDisconnected(anyString());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void shouldHandleNullEmailInLogout() {
        assertThrows(BadRequestException.class, () -> authenticationService.logout(refreshTokenRequest, null));
        verify(refreshTokenRepository, never()).findByTokenAndUserEmail(anyString(), anyString());
    }

    @Test
    void shouldHandleNullRefreshTokenInLogout() {
        RefreshTokenRequest nullTokenRequest = new RefreshTokenRequest(null);

        assertThrows(BadRequestException.class,
                () -> authenticationService.logout(nullTokenRequest, email));

        verify(refreshTokenRepository, never()).findByTokenAndUserEmail(anyString(), anyString());
    }

    @Test
    void shouldHandleNullRefreshTokenInRefreshToken() {
        RefreshTokenRequest nullTokenRequest = new RefreshTokenRequest(null);

        assertThrows(BadRequestException.class,
                () -> authenticationService.refreshToken(nullTokenRequest));

        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void shouldHandleNullUserDetailsInRefreshToken() {
        when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken())).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(null);

        assertThrows(BadRequestException.class,
                () -> authenticationService.refreshToken(refreshTokenRequest));

        verify(jwtService, never()).refreshToken(anyString());
    }

}