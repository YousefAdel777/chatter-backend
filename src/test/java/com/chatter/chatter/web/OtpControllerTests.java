package com.chatter.chatter.web;

import com.chatter.chatter.controller.OtpController;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.request.OtpSendRequest;
import com.chatter.chatter.request.OtpVerificationRequest;
import com.chatter.chatter.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(OtpController.class)
public class OtpControllerTests extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OtpService otpService;

    private String token;
    private String otp;

    @BeforeEach
    public void setup() {
        token = UUID.randomUUID().toString();
        otp = "123456";
    }

    @Test
    void verifyOtp_ShouldReturnToken_WhenValidOtp() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest(otp, user.getEmail());
        when(otpService.verifyOtp(user.getEmail(), otp)).thenReturn(token);
        mockMvc.perform(post("/api/otp/verification")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void verifyOtp_ShouldReturnVadRequest_WhenWrongOtp() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest(otp, user.getEmail());
        doThrow(new BadRequestException("error", "test exception")).when(otpService).verifyOtp(user.getEmail(), otp);
        mockMvc.perform(post("/api/otp/verification")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_ShouldReturnToken_WhenInvalidRequest() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest();
        mockMvc.perform(post("/api/otp/verification")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendOtp_ShouldSendOtp_WhenValidRequest() throws Exception {
        OtpSendRequest request = new OtpSendRequest(user.getEmail());
        mockMvc.perform(post("/api/otp/send-otp")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void sendOtp_ShouldReturnBadRequest_WhenInValidRequest() throws Exception {
        OtpSendRequest request = new OtpSendRequest();
        mockMvc.perform(post("/api/otp/send-otp")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
