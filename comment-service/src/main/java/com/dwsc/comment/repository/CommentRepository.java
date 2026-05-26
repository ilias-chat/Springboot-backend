package com.dwsc.comment.repository;

import com.dwsc.comment.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    Page<Comment> findByAuthorOrderByCreatedAtDesc(String author, Pageable pageable);

    long countByAuthor(String author);
}

