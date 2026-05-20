package com.dwsc.backend.repository;

import com.dwsc.backend.model.entity.PlayerComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerCommentRepository extends JpaRepository<PlayerComment, UUID> {}
