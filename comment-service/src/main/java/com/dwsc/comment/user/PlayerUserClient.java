package com.dwsc.comment.user;

import com.dwsc.comment.config.FeignAuthForwardingConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Loads synced profile from player-service (same DB row as Node {@code User.findOne}).
 * Cloud Run: set {@code PLAYER_SERVICE_URL} to the player-service HTTPS base.
 */
@FeignClient(
        name = "dwsc-player",
        url = "${PLAYER_SERVICE_URL:}",
        configuration = FeignAuthForwardingConfig.class)
public interface PlayerUserClient {
    @GetMapping("/api/users/me")
    UserProfileResponse getMe();
}
