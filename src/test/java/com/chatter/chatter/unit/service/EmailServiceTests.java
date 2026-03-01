package com.chatter.chatter.unit.service;

import com.chatter.chatter.service.EmailService;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EmailServiceTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private final String FROM_EMAIL = "noreply@chatter.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
    }

    @Test
    void sendOtpEmail_ShouldProcessTemplateAndSendEmail() throws Exception {
        String toEmail = "user@example.com";
        String username = "TestUser";
        String otp = "123456";
        String expectedHtml = "<html>OTP: 123456</html>";

        when(templateEngine.process(eq("otp-email"), any(Context.class))).thenReturn(expectedHtml);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail(toEmail, username, otp);

        verify(templateEngine).process(eq("otp-email"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

}
