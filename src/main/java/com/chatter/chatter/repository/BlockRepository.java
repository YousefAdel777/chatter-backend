package com.chatter.chatter.repository;

import com.chatter.chatter.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findAllByBlockedByEmail(String email);
    Optional<Block> findByBlockedByEmailAndBlockedUserId(String email, Long userId);
    Optional<Block> findByIdAndBlockedByEmail(Long blockId, String email);
    boolean existsByBlockedByEmailAndBlockedUserId(String email, Long userId);
    boolean existsByBlockedByIdAndBlockedUserEmail(Long userId, String email);
}
