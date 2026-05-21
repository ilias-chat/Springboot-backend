package com.dwsc.backend.player;

import com.dwsc.backend.model.GeoJsonPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerServiceLocationTest {

    @Test
    void resolvePlayerLocation_prefersStadium() {
        var stadium = new GeoJsonPoint("Point", List.of(2.0, 41.0));
        var result = PlayerService.resolvePlayerLocation(stadium, 51.5, -0.1, true);
        assertEquals(stadium, result);
    }

    @Test
    void resolvePlayerLocation_usesDeviceWhenStadiumNull() {
        var result = PlayerService.resolvePlayerLocation(null, 41.0, 2.0, true);
        assertEquals(new GeoJsonPoint("Point", List.of(2.0, 41.0)), result);
    }

    @Test
    void resolvePlayerLocation_nullWithoutDevice() {
        assertNull(PlayerService.resolvePlayerLocation(null, null, null, false));
    }
}
