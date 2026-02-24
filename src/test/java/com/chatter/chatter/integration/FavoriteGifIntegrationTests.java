package com.chatter.chatter.integration;

import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.FavoriteGifRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.FavoriteGifRequest;
import com.chatter.chatter.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class FavoriteGifIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private FavoriteGifRepository favoriteGifRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User user1;

    private User user2;

    private FavoriteGif favoriteGif1;

    private FavoriteGif favoriteGif2;

    private FavoriteGif favoriteGif3;

    private String accessToken;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        favoriteGifRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("testUser1")
                .password("testPassword")
                .email("test1@example.com")
                .build());

        user2 = userRepository.save(User.builder()
                .username("testUser2")
                .password("testPassword")
                .email("test2@example.com")
                .build());

        favoriteGif1 = favoriteGifRepository.save(FavoriteGif.builder()
                .user(user1)
                .gifId(UUID.randomUUID().toString()).build());

        favoriteGif2 = favoriteGifRepository.save(FavoriteGif.builder()
                .user(user1)
                .gifId(UUID.randomUUID().toString()).build());

        favoriteGif3 = favoriteGifRepository.save(FavoriteGif.builder()
                .user(user2)
                .gifId(UUID.randomUUID().toString()).build());

        accessToken = jwtService.generateToken(user1.getEmail()).getAccessToken();
    }

    @Test
    public void getFavoriteGifs_ShouldReturnPaginatedFavoriteGifsSortedByCreationDateInDescendingOrder_WhenDataExists() {
        String gifId1 =  favoriteGif1.getGifId();
        String gifId2 =  favoriteGif2.getGifId();
        webClient
                .get()
                .uri("/api/favorite-gifs")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.number").isEqualTo(0)
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].userId").isEqualTo(user1.getId())
                .jsonPath("$.content[0].gifId").isEqualTo(gifId2)
                .jsonPath("$.content[1].userId").isEqualTo(user1.getId())
                .jsonPath("$.content[1].gifId").isEqualTo(gifId1);
    }

    @Test
    public void getFavoriteGifs_ShouldReturnFavoriteGifs_WhenDataExists() {
        webClient
                .get()
                .uri(builder -> builder.path("/api/favorite-gifs/batch").queryParam("gifIds", favoriteGif1.getGifId()).build())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].gifId").isEqualTo(favoriteGif1.getGifId());
    }

    @Test
    public void getGifStatus_ShouldReturnTrue_WhenExists() {
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/favorite-gifs/status").queryParam("gifId", favoriteGif1.getGifId()).build())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isFavorite").isEqualTo(true);

    }

    @Test
    public void getGifStatus_ShouldReturnFalse_WhenNoMatchExists() {
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/favorite-gifs/status").queryParam("gifId", favoriteGif3.getGifId()).build())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isFavorite").isEqualTo(false);
    }

    @Test
    public void createFavoriteGif_ShouldCreateFavoriteGif_WhenValid() {
        String gifId4 = UUID.randomUUID().toString();

        FavoriteGifRequest request = new  FavoriteGifRequest(gifId4);

        webClient
                .post()
                .uri("/api/favorite-gifs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), FavoriteGifRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(user1.getId())
                .jsonPath("$.gifId").isEqualTo(gifId4)
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();

        webClient
                .get()
                .uri(builder -> builder.path("/api/favorite-gifs/status").queryParam("gifId", gifId4).build())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isFavorite").isEqualTo(true);
    }

    @Test
    public void createFavoriteGif_ShouldReturnBadRequest_WhenDuplicateGifIds() {
        FavoriteGifRequest request = new FavoriteGifRequest(favoriteGif1.getGifId());
        webClient
                .post()
                .uri("/api/favorite-gifs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), FavoriteGifRequest.class)
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    public void deleteFavoriteGif_ShouldDeleteGif_WhenFound() {
        webClient
                .delete()
                .uri("/api/favorite-gifs/" + favoriteGif1.getGifId())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isNoContent();

        webClient
                .get()
                .uri(builder -> builder.path("/api/favorite-gifs/status").queryParam("gifId", favoriteGif1.getGifId()).build())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isFavorite").isEqualTo(false);
    }

    @Test
    public void deleteFavoriteGif_ShouldNotDeleteFavoriteGif_WhenNotFound() {
        webClient
                .delete()
                .uri("/api/favorite-gifs/" + favoriteGif3.getGifId())
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

}
