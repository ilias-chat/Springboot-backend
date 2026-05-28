package com.dwsc.comment.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Subset of player-service {@code UserResponse} for author display name resolution. */
public record UserProfileResponse(
        String name,
        String email,
        @JsonProperty("firebaseUID") String firebaseUid) {}
