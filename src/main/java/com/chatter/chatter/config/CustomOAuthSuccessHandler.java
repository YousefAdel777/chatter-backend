package com.chatter.chatter.config;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {


    @Value("${oauth-redirect-url}")
    private String redirectUrl;
    private final JwtService jwtService;

    @Autowired
    public CustomOAuthSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String email = (String) request.getAttribute("email");
        TokenDto tokenDto = jwtService.generateToken(email);
        String code = jwtService.generateCode(tokenDto);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl + "?code=" + code);
    }
}
