package com.dwsc.backend.model.entity;

import com.dwsc.backend.model.GeoJsonPoint;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors TRWM-backend {@code models/Player.js} (comments stored as separate rows).
 */
@Entity
@Table(
        name = "players",
        indexes = {
                @Index(name = "idx_players_name", columnList = "name"),
                @Index(name = "idx_players_team", columnList = "team"),
                @Index(name = "idx_players_league", columnList = "league"),
                @Index(name = "idx_players_position", columnList = "position"),
                @Index(name = "idx_players_registration_date", columnList = "registration_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String team;

    @Column(nullable = false, length = 200)
    private String league;

    @Column(columnDefinition = "text")
    private String image;

    @Column(length = 64)
    private String position;

    /** API-Football stats blob (Mongo Mixed). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode stats;

    @Column(name = "venue_name", length = 300)
    private String venueName;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate;

    /** API-Football player id; unique when not null (sparse unique in Mongo). */
    @Column(name = "external_id", unique = true)
    private Integer externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private GeoJsonPoint location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (registrationDate == null) {
            registrationDate = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
