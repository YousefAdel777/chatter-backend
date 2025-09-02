package com.chatter.chatter.repository;

import com.chatter.chatter.model.React;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReactRepository extends JpaRepository<React, Long> {
    Optional<React> findByIdAndUserEmail(Long reactId, String email);
    Boolean existsByMessageIdAndUserEmail(Long messageId, String email);
}
