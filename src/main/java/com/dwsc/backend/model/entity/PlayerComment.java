package com.dwsc.backend.model.entity;

import com.dwsc.backend.model.GeoJsonPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Embedded comment in Mongo → own table. Matches {@code commentSchema} in TRWM-backend
 * {@code models/Player.js}.
 */
@Entity
@Table(
        name = "player_comments",
        indexes = {
                @Index(name = "idx_pc_player_id", columnList = "player_id"),
                @Index(name = "idx_pc_author", columnList = "author")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PlayerComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, length = 128)
    private String author;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private Integer rating;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private GeoJsonPoint location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
