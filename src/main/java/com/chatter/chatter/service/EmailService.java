package com.chatter.chatter.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String username, String otp) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("otp", otp);

        String processed = templateEngine.process("otp-email", context);
        String subject = String.format("%s is your Chatter verification code", otp);
        sendHtmlEmail(toEmail, subject, processed);
    }

    private void sendHtmlEmail(String toEmail, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("Chatter <" + fromEmail + ">");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

}
