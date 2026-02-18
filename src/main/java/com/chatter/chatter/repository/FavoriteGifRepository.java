package com.chatter.chatter.repository;

import com.chatter.chatter.model.FavoriteGif;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FavoriteGifRepository extends JpaRepository<FavoriteGif, Long> {

    boolean existsByUserIdAndGifId(Long userId, String gifId);

    Page<FavoriteGif> findAllByUserId(Long userId, Pageable pageable);

    List<FavoriteGif> findAllByUserIdAndGifIdIn(Long userId, Collection<String> gifIds);

    @Modifying
    @Query("""
        DELETE FROM FavoriteGif f WHERE f.user.id = :userId AND f.gifId = :gifId
    """)
    int deleteByGifIdAndUserId(String gifId, Long userId);

}
