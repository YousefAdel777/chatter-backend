package com.chatter.chatter.controller;

import com.chatter.chatter.dto.BlockDto;
import com.chatter.chatter.dto.BlockPostRequest;
import com.chatter.chatter.mapper.BlockMapper;
import com.chatter.chatter.model.Block;
import com.chatter.chatter.service.BlockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;
    private final BlockMapper blockMapper;

    public BlockController(
            BlockService blockService,
            BlockMapper blockMapper
    ) {
        this.blockService = blockService;
        this.blockMapper = blockMapper;
    }

    @GetMapping
    public ResponseEntity<List<BlockDto>> getBlocks(Principal principal) {
        List<Block> blocks = blockService.getUserBlocks(principal.getName());
        return ResponseEntity.ok(blockMapper.toDtoList(blocks));
    }

    @GetMapping("/me")
    public ResponseEntity<BlockDto> getUserBlock(
            Principal principal,
            @RequestParam Long userId
    ) {
        Block block = blockService.getUserBlock(principal.getName(), userId);
        return ResponseEntity.ok(blockMapper.toDto(block));
    }

    @GetMapping("/blocked")
    public ResponseEntity<Map<String, Boolean>> isBlocked(
            Principal principal,
            @RequestParam Long userId
    ) {
        boolean isBlocked = blockService.isBlocked(principal.getName(), userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBlocked", isBlocked);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{blockId}")
    public ResponseEntity<BlockDto> getBlock(Principal principal, @PathVariable Long blockId) {
        Block block = blockService.getBlock(principal.getName(), blockId);
        return ResponseEntity.ok(blockMapper.toDto(block));
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
