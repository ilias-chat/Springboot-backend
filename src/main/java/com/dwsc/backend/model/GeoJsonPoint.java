package com.dwsc.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON Point (RFC 7947): {@code coordinates} are {@code [longitude, latitude]}.
 * Matches TRWM-backend nested location schema in {@code models/Player.js}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoJsonPoint {

    private String type = "Point";
    private List<Double> coordinates;
}
