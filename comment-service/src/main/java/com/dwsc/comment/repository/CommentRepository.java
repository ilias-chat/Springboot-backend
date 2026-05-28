package com.dwsc.comment.repository;

import com.dwsc.comment.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    Page<Comment> findByAuthorOrderByCreatedAtDesc(String author, Pageable pageable);

    long countByAuthor(String author);

    @Query(
            """
            SELECT c.playerId, COUNT(c), COALESCE(AVG(c.rating), 0)
            FROM Comment c
            WHERE c.playerId IN :playerIds
            GROUP BY c.playerId
            """)
    List<Object[]> summarizeByPlayerIds(@Param("playerIds") Collection<UUID> playerIds);
}

