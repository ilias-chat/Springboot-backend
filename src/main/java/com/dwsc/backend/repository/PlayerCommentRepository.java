package com.dwsc.backend.repository;

import com.dwsc.backend.model.entity.PlayerComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayerCommentRepository extends JpaRepository<PlayerComment, UUID> {

    @Query(
            "SELECT c FROM PlayerComment c JOIN FETCH c.player WHERE c.author = :author ORDER BY c.createdAt DESC")
    List<PlayerComment> findByAuthorWithPlayer(@Param("author") String author, Pageable pageable);

    long countByAuthor(String author);
}
