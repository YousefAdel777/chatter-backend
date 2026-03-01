package com.chatter.chatter.service;

import com.chatter.chatter.model.UserStatus;
import com.chatter.chatter.request.RefreshTokenRequest;
import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.request.UserLoginRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.repository.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final OnlineUserService onlineUserService;
    private final UserService userService;

    public TokenDto login(UserLoginRequest userLoginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                userLoginRequest.getEmail(),
                userLoginRequest.getPassword()
        ));
        return jwtService.generateToken(authentication.getName());
    }

    public TokenDto refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        }
        catch (MalformedJwtException | ExpiredJwtException e) {
            throw new BadRequestException("refreshToken", "Invalid refresh token");
        }
        if (username == null) {
            throw new BadRequestException("refreshToken", "Invalid refresh token");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        System.out.println(userDetails.getUsername());
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException("message", "Invalid refresh token");
        }
        return jwtService.refreshToken(refreshToken);
    }

    public void logout(RefreshTokenRequest refreshTokenRequest, String email) {
        String token = refreshTokenRequest.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndUserEmail(token, email)
                .orElseThrow(() -> new BadRequestException("refreshToken", "Invalid token"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isRefreshTokenValid(token, userDetails)) {
            throw new BadRequestException("refreshToken", "Invalid token");
        }
        onlineUserService.userDisconnected(email);
        refreshTokenRepository.delete(refreshToken);
    }

    public TokenDto verifyUser(String token) {
        String email = userService.updateUserStatus(token, UserStatus.VERIFIED);
        return jwtService.generateToken(email);
    }

}
