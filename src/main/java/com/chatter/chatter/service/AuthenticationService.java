package com.chatter.chatter.service;

import com.chatter.chatter.dto.RefreshTokenDto;
import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.dto.UserLoginDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final OnlineUserService onlineUserService;

    public TokenDto login(UserLoginDto userLoginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                userLoginDto.getEmail(),
                userLoginDto.getPassword()
        ));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(userLoginDto.getEmail());
        }
        return null;
    }

    public TokenDto refreshToken(RefreshTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException("message", "Invalid refresh token");
        }
        return jwtService.refreshToken(refreshToken);
    }

    public void logout(RefreshTokenDto refreshTokenDto, Principal principal) {
        String token = refreshTokenDto.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndUserEmail(token, principal.getName())
                .orElseThrow(() -> new BadRequestException("refreshToken", "Invalid token"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getName());
        if (!jwtService.isRefreshTokenValid(token, userDetails)) {
            throw new BadRequestException("refreshToken", "Invalid token");
        }
        onlineUserService.userDisconnected(principal.getName());
        refreshTokenRepository.delete(refreshToken);
    }

}
