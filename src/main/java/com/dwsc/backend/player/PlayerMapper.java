package com.dwsc.backend.player;

import com.dwsc.backend.api.dto.PlayerCommentResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.model.entity.Player;
import com.dwsc.backend.model.entity.PlayerComment;

import java.util.List;

public final class PlayerMapper {

    private PlayerMapper() {}

    public static PlayerResponse toResponse(Player player, boolean includeComments) {
        List<PlayerCommentResponse> comments = null;
        if (includeComments && player.getComments() != null && !player.getComments().isEmpty()) {
            comments = player.getComments().stream().map(PlayerMapper::toCommentResponse).toList();
        }
        return new PlayerResponse(
                player.getId().toString(),
                player.getName(),
                player.getTeam(),
                player.getLeague(),
                player.getImage(),
                player.getPosition(),
                player.getStats(),
                player.getVenueName(),
                player.getRegistrationDate(),
                player.getExternalId(),
                player.getLocation(),
                comments,
                player.getCreatedAt(),
                player.getUpdatedAt());
    }

    public static PlayerCommentResponse toCommentResponse(PlayerComment comment) {
        return new PlayerCommentResponse(
                comment.getId().toString(),
                comment.getAuthor(),
                comment.getAuthorName(),
                comment.getText(),
                comment.getRating(),
                comment.getLocation(),
                comment.getCreatedAt());
    }
}
