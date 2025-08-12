package com.chatter.chatter.controller;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.service.OnlineUserService;
import com.chatter.chatter.service.TypingUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;
import java.util.List;

@Controller
@CrossOrigin
@RequiredArgsConstructor
public class WebsocketController {

    private final OnlineUserService onlineUserService;
    private final TypingUserService typingUserService;
    private final UserMapper userMapper;

    @SubscribeMapping("/users.{userId}.status")
    public Boolean sendUserOnline(@DestinationVariable Long userId) {
        return onlineUserService.isOnline(userId);
    }

    @MessageMapping("/signal")
    @SendTo("/topic/signal")
    public String handleSignal(String signal) {
       return signal;
    }

    @SubscribeMapping("/chat.{chatId}.typing-users")
    public List<UserDto> getTypingUsers(@DestinationVariable Long chatId, Principal principal) {
        return userMapper.toDtoList(typingUserService.getTypingUsers(chatId, principal));
    }

    @MessageMapping("/chat.{chatId}.add-typing-users")
    public void addTypingUser(@DestinationVariable Long chatId, Principal principal) {
        typingUserService.addTypingUser(chatId, principal);
    }


    @MessageMapping("/chat.{chatId}.remove-typing-users")
    public void removeTypingUser(@DestinationVariable Long chatId, Principal principal) {
        typingUserService.removeTypingUser(chatId, principal);
    }


}
