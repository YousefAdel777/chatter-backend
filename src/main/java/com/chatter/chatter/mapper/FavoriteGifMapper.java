package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.FavoriteGifDto;
import com.chatter.chatter.model.FavoriteGif;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FavoriteGifMapper {

    public FavoriteGifDto toDto(FavoriteGif favoriteGif) {
        if (favoriteGif == null) return null;
        return FavoriteGifDto.builder()
                .id(favoriteGif.getId())
                .gifId(favoriteGif.getGifId())
                .userId(favoriteGif.getUser().getId())
                .createdAt(favoriteGif.getCreatedAt())
                .build();
    }

    public List<FavoriteGifDto> toDtoList(List<FavoriteGif> favoriteGifs) {
        if (favoriteGifs == null) return null;
        return favoriteGifs.stream().map(this::toDto).collect(Collectors.toList());
    }

}
