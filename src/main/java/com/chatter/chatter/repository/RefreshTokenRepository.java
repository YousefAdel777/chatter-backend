package com.chatter.chatter.repository;

import com.chatter.chatter.model.Option;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Set<RefreshToken> findAllByUserEmail(String email);

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndUserEmail(String token, String email);

}