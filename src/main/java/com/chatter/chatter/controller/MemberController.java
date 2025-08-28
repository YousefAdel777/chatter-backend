package com.chatter.chatter.controller;

import com.chatter.chatter.dto.MemberDto;
import com.chatter.chatter.dto.MemberPatchRequest;
import com.chatter.chatter.mapper.MemberMapper;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @GetMapping
    public ResponseEntity<List<MemberDto>> getMembers(
            @RequestParam Long chatId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(memberService.getMembersByChat(chatId, username, email));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberDto> getCurrentMember(
            Principal principal,
            @RequestParam Long chatId
    ) {
        return ResponseEntity.ok(memberService.getCurrentChatMember(principal.getName(), chatId));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberDto> getMember(@PathVariable  Long memberId) {
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable  Long memberId, Principal principal) {
        memberService.deleteMember(principal.getName(), memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberDto> updateMember(
            @PathVariable  Long memberId,
            @Valid @RequestBody MemberPatchRequest request,
            Principal principal
    ) {
        Member member = memberService.updateMember(principal.getName(), memberId, request);
        return ResponseEntity.ok(memberMapper.toDto(member));
    }

}
