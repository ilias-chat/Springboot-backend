package com.dwsc.backend.player;

import com.dwsc.backend.api.dto.PlayerCommentResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.model.entity.Player;

import java.util.List;

public final class PlayerMapper {

    private PlayerMapper() {}

    public static PlayerResponse toResponse(Player player, List<PlayerCommentResponse> comments) {
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
}
