package com.chatter.chatter.controller;

import com.chatter.chatter.dto.VoteDto;
import com.chatter.chatter.dto.VotePostRequest;
import com.chatter.chatter.mapper.VoteMapper;
import com.chatter.chatter.model.Vote;
import com.chatter.chatter.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/votes")
@CrossOrigin
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final VoteMapper voteMapper;


    @PostMapping
    public ResponseEntity<List<VoteDto>> createVotes(
            @RequestBody @Valid VotePostRequest votePostRequest,
            Principal principal
    ) {
        List<Vote> votes = voteService.createVotes(principal.getName(), votePostRequest.getOptionsIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(voteMapper.toDtoList(votes));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteVotes(
            @RequestParam("messageId") Long messageId,
            Principal principal
    ) {
        voteService.deleteVotesByMessage(principal.getName(), messageId);
        return ResponseEntity.noContent().build();
    }

}
