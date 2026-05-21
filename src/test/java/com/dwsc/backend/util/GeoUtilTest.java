package com.dwsc.backend.util;

import com.dwsc.backend.model.GeoJsonPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeoUtilTest {

    @Test
    void resolvePlayerLocation_prefersStadium() {
        var stadium = new GeoJsonPoint("Point", List.of(2.0, 41.0));
        var result = GeoUtil.resolvePlayerLocation(stadium, 51.5, -0.1, true);
        assertEquals(stadium, result);
    }

    @Test
    void resolvePlayerLocation_usesDeviceWhenStadiumNull() {
        var result = GeoUtil.resolvePlayerLocation(null, 41.0, 2.0, true);
        assertEquals(new GeoJsonPoint("Point", List.of(2.0, 41.0)), result);
    }

    @Test
    void resolvePlayerLocation_nullWithoutDevice() {
        assertNull(GeoUtil.resolvePlayerLocation(null, null, null, false));
    }
}
