package com.chatter.chatter.controller;

import com.chatter.chatter.dto.ReactDto;
import com.chatter.chatter.request.ReactPatchRequest;
import com.chatter.chatter.request.ReactPostRequest;
import com.chatter.chatter.mapper.ReactMapper;
import com.chatter.chatter.model.React;
import com.chatter.chatter.service.ReactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/reacts")
@RequiredArgsConstructor
public class ReactController {

    private final ReactService reactService;
    private final ReactMapper reactMapper;

    @PostMapping
    public ResponseEntity<ReactDto> createReact(@Valid @RequestBody ReactPostRequest reactPostRequest, Principal principal) {
        React react = reactService.createReact(principal, reactPostRequest.getMessageId(), reactPostRequest.getEmoji());
        ReactDto reactDto = reactMapper.toDto(react);
        return ResponseEntity.status(HttpStatus.CREATED).body(reactDto);
    }

    @DeleteMapping("/{reactId}")
    public ResponseEntity<Void> deleteReact(@PathVariable Long reactId, Principal principal) {
        reactService.deleteReact(principal.getName(), reactId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reactId}")
    public ResponseEntity<ReactDto> updateReact(@PathVariable Long reactId, @Valid @RequestBody ReactPatchRequest reactPatchRequest, Principal principal) {
        React react = reactService.updateReact(reactId, principal.getName(), reactPatchRequest.getEmoji());
        ReactDto reactDto = reactMapper.toDto(react);
        return ResponseEntity.ok(reactDto);
    }

}
