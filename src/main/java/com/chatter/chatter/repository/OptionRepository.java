package com.chatter.chatter.repository;

import com.chatter.chatter.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {

    @Query("""
        SELECT o FROM Option o
        JOIN PollMessage pm ON o.pollMessage = pm
        WHERE o.id IN :optionsIds
        AND EXISTS (
            SELECT 1 FROM Member m
            WHERE m.chat.id = pm.chat.id
            AND m.user.email = :email
        )
        AND NOT EXISTS (
            SELECT 1 FROM Vote v
            WHERE v.user.email = :email
            AND v.option.id = o.id
        )
    """)
    List<Option> findOptionsWithoutVotes(@Param("email") String email, @Param("optionsIds") Iterable<Long> optionsIds);

}
