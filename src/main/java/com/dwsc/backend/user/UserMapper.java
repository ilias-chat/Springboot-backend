package com.dwsc.backend.user;

import com.dwsc.backend.api.dto.UserResponse;
import com.dwsc.backend.model.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId().toString(),
                u.getFirebaseUid(),
                u.getEmail(),
                u.getRole().name(),
                u.getName(),
                u.getAvatar(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
