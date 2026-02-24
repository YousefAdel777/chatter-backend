package com.chatter.chatter.unit.controller;

import com.chatter.chatter.controller.FavoriteGifController;
import com.chatter.chatter.dto.FavoriteGifDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.mapper.FavoriteGifMapper;
import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.User;
import com.chatter.chatter.model.UserPrincipal;
import com.chatter.chatter.request.FavoriteGifRequest;
import com.chatter.chatter.service.FavoriteGifService;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(FavoriteGifController.class)
@ActiveProfiles("test")
@WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
public class FavoriteGifControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FavoriteGifMapper favoriteGifMapper;

    @MockitoBean
    private FavoriteGifService favoriteGifService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User user;

    private String gifId1;

    private String gifId2;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .id(1L)
                .username("testUser")
                .password("testPassword")
                .email("test@example.com")
                .build();
        gifId1 = UUID.randomUUID().toString();
        gifId2 = UUID.randomUUID().toString();
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(new UserPrincipal(user));
    }

    @Test
    public void getFavoriteGifs_ShouldReturnPaginatedFavoriteGifs_WhenDataExists() throws Exception {
        FavoriteGifDto favoriteGifDto1 = new FavoriteGifDto(1L, gifId1, user.getId(), Instant.now());
        FavoriteGifDto favoriteGifDto2 = new FavoriteGifDto(2L, gifId2, user.getId(), Instant.now());
        PageDto<FavoriteGifDto> pageDto = new PageDto<>(Arrays.asList(favoriteGifDto1, favoriteGifDto2), 2L);
        when(favoriteGifService.getFavoriteGifs(eq(user.getId()), any(Pageable.class))).thenReturn(pageDto);
        mockMvc.perform(get("/api/favorite-gifs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2));
    }

    @Test
    public void getFavoriteGifs_ShouldReturnEmptyPage_WhenNoData() throws Exception {
        PageDto<FavoriteGifDto> emptyPage = new PageDto<>(List.of(), 0L);
        when(favoriteGifService.getFavoriteGifs(anyLong(), any(Pageable.class))).thenReturn(emptyPage);
        mockMvc.perform(get("/api/favorite-gifs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    public void getFavoriteGifs_ShouldReturnFavoriteGifs_WhenDataExists() throws Exception {
        FavoriteGifDto favoriteGifDto1 = new FavoriteGifDto(1L, gifId1, user.getId(), Instant.now());
        FavoriteGifDto favoriteGifDto2 = new FavoriteGifDto(2L, gifId2, user.getId(), Instant.now());
        when(favoriteGifService.getFavoriteGifs(eq(user.getId()), anyList())).thenReturn(Arrays.asList(favoriteGifDto1, favoriteGifDto2));
        mockMvc.perform(get("/api/favorite-gifs/batch")
                        .param("gifIds", gifId1, gifId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    public void getGifStatus_ShouldReturnBadRequest_WhenGifIdIsNotProvided() throws Exception {
        mockMvc.perform(get("/api/favorite-gifs/status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getGifStatus_ShouldReturnTrue_WhenExists() throws Exception {
        when(favoriteGifService.getGifStatus(user.getId(), gifId1)).thenReturn(true);
        mockMvc.perform(get("/api/favorite-gifs/status")
                .param("gifId", gifId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));
    }

    @Test
    public void getGifStatus_ShouldReturnFalse_WhenNoMatchExists() throws Exception {
        when(favoriteGifService.getGifStatus(user.getId(), gifId1)).thenReturn(false);
        mockMvc.perform(get("/api/favorite-gifs/status")
                        .param("gifId", gifId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    public void createFavoriteGif_ShouldCreateFavoriteGif_WhenValid() throws Exception {
        FavoriteGifRequest favoriteGifRequest = new FavoriteGifRequest(gifId1);
        FavoriteGif favoriteGif = new FavoriteGif(1L, gifId1, user, Instant.now());
        FavoriteGifDto favoriteGifDto = new FavoriteGifDto(1L, gifId1, user.getId(), Instant.now());
        when(favoriteGifService.createFavoriteGif(eq(user.getId()), any(FavoriteGifRequest.class))).thenReturn(favoriteGif);
        when(favoriteGifMapper.toDto(favoriteGif)).thenReturn(favoriteGifDto);
        mockMvc.perform(post("/api/favorite-gifs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(favoriteGifRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.gifId").value(gifId1))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    public void createFavoriteGif_ShouldReturnBadRequest_WhenInvalid() throws Exception {
        FavoriteGifRequest favoriteGifRequest = new FavoriteGifRequest();
        mockMvc.perform(post("/api/favorite-gifs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(favoriteGifRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createFavoriteGif_ShouldReturnBadRequest_WhenDuplicateGifIds() throws Exception {
        FavoriteGifRequest favoriteGifRequest = new FavoriteGifRequest(gifId1);
        when(favoriteGifService.createFavoriteGif(eq(user.getId()), any(FavoriteGifRequest.class))).thenThrow(new BadRequestException("exception", "test exception"));
        mockMvc.perform(post("/api/favorite-gifs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(favoriteGifRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteFavoriteGif_ShouldDeleteGif_WhenFound() throws Exception {
        mockMvc.perform(delete("/api/favorite-gifs/" + gifId1)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

}
