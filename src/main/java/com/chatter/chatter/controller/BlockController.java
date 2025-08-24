package com.chatter.chatter.controller;

import com.chatter.chatter.dto.BlockDto;
import com.chatter.chatter.request.BlockPostRequest;
import com.chatter.chatter.mapper.BlockMapper;
import com.chatter.chatter.model.Block;
import com.chatter.chatter.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;
    private final BlockMapper blockMapper;

    @GetMapping
    public ResponseEntity<List<BlockDto>> getBlocks(Principal principal) {
        return ResponseEntity.ok(blockService.getUserBlocks(principal.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<BlockDto> getUserBlock(
            Principal principal,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(blockService.getUserBlock(principal.getName(), userId));
    }

    @GetMapping("/blocked")
    public ResponseEntity<Map<String, Boolean>> isBlocked(
            Principal principal,
            @RequestParam Long userId
    ) {
        boolean isBlocked = blockService.isBlocked(principal.getName(), userId);
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }

    @GetMapping("/{blockId}")
    public ResponseEntity<BlockDto> getBlock(Principal principal, @PathVariable Long blockId) {
        return ResponseEntity.ok(blockService.getBlock(principal.getName(), blockId));
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteBlock(Principal principal, @PathVariable Long blockId) {
        blockService.deleteBlock(principal.getName(), blockId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<BlockDto> createBlock(
            Principal principal,
            @Valid @RequestBody BlockPostRequest blockPostRequest
        ) {
        Block block = blockService.createBlock(principal.getName(), blockPostRequest.getBlockedUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockMapper.toDto(block));
    }
}
