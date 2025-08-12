package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.BlockDto;
import com.chatter.chatter.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BlockMapper {

    private final UserMapper userMapper;

    public BlockDto toDto(Block block) {
        if (block == null) return null;
        return BlockDto.builder()
                .id(block.getId())
                .blockedBy(userMapper.toDto(block.getBlockedBy()))
                .blockedUser(userMapper.toDto(block.getBlockedUser()))
                .build();
    }

    public List<BlockDto> toDtoList(List<Block> blocks) {
        if (blocks == null) return null;
        return blocks.stream().map(this::toDto).collect(Collectors.toList());
    }

}
