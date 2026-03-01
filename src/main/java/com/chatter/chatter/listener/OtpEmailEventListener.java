package com.chatter.chatter.listener;

import com.chatter.chatter.config.RabbitMQConfig;
import com.chatter.chatter.event.OtpEmailEvent;
import com.chatter.chatter.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpEmailEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.OTP_EMAILS)
    public void handleEvent(@Payload @Valid OtpEmailEvent event) {
        try {
            emailService.sendOtpEmail(event.getEmail(), event.getUsername(), event.getOtp());
        }
        catch (MailSendException e) {
            throw e;
        }
        catch (MessagingException | MailAuthenticationException e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
        catch (Exception e) {
            log.error("Error sending OTP to {}", event.getEmail(), e);
        }
    }

}
