package com.dwsc.backend.util;

import com.dwsc.backend.model.GeoJsonPoint;

import java.util.List;

public final class GeoUtil {

    private GeoUtil() {}

    public static boolean hasDeviceCoords(Double lat, Double lng) {
        return lat != null
                && lng != null
                && Double.isFinite(lat)
                && Double.isFinite(lng)
                && lat >= -90
                && lat <= 90
                && lng >= -180
                && lng <= 180;
    }

    /** Stadium coords from API-Football when present; otherwise device GPS when {@code useDeviceLocation}. */
    public static GeoJsonPoint resolvePlayerLocation(
            GeoJsonPoint stadiumLocation, Double lat, Double lng, boolean useDeviceLocation) {
        if (stadiumLocation != null) {
            return stadiumLocation;
        }
        if (useDeviceLocation) {
            return new GeoJsonPoint("Point", List.of(lng, lat));
        }
        return null;
    }
}
