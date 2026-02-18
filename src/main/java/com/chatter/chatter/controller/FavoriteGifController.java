package com.chatter.chatter.controller;

import com.chatter.chatter.dto.FavoriteGifDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.mapper.FavoriteGifMapper;
import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.UserPrincipal;
import com.chatter.chatter.request.FavoriteGifRequest;
import com.chatter.chatter.service.FavoriteGifService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite-gifs")
public class FavoriteGifController {

    private final FavoriteGifService favoriteGifService;
    private final FavoriteGifMapper favoriteGifMapper;

    @GetMapping
    public ResponseEntity<Page<FavoriteGifDto>> getFavoriteGifs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageDto<FavoriteGifDto> pageDto = favoriteGifService.getFavoriteGifs(principal.getUser().getId(), pageable);
        return ResponseEntity.ok(new PageImpl<>(pageDto.getContent(), pageable, pageDto.getTotalElements()));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<FavoriteGifDto>> getFavoriteGifs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("gifIds") List<String> gifIds
    ) {
        List<FavoriteGifDto> favoriteGifDtos = favoriteGifService.getFavoriteGifs(principal.getUser().getId(), gifIds);
        return ResponseEntity.ok(favoriteGifDtos);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getFavoriteGifStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("gifId") String gifId
    ) {
        boolean status = favoriteGifService.getGifStatus(principal.getUser().getId(), gifId);
        return ResponseEntity.ok(Map.of("isFavorite",  status));
    }

    @PostMapping
    public ResponseEntity<FavoriteGifDto> createFavoriteGif(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FavoriteGifRequest request
    ) {
        FavoriteGif favoriteGif = favoriteGifService.createFavoriteGif(principal.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteGifMapper.toDto(favoriteGif));
    }

    @DeleteMapping("/{gifId}")
    public ResponseEntity<Void> deleteFavoriteGif(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String gifId
    ) {
        favoriteGifService.deleteFavoriteGif(principal.getUser().getId(),  gifId);
        return ResponseEntity.noContent().build();
    }

}
