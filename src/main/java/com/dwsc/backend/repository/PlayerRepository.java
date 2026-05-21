package com.dwsc.backend.repository;

import com.dwsc.backend.model.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByExternalId(Integer externalId);

    @Query("SELECT DISTINCT p FROM Player p LEFT JOIN FETCH p.comments WHERE p.id = :id")
    Optional<Player> findByIdWithComments(@Param("id") UUID id);

    /**
     * Cast to text before lower() so PostgreSQL works when legacy columns are still bytea
     * (Cloud SQL shared with Node/Mongo migration). Prefer scripts/postgresql-fix-player-text-columns.sql.
     */
    @Query(
            value =
                    """
                    SELECT p.* FROM players p
                    WHERE lower(cast(p.name as text)) LIKE lower(concat('%', :q, '%'))
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM players p
                    WHERE lower(cast(p.name as text)) LIKE lower(concat('%', :q, '%'))
                    """,
            nativeQuery = true)
    Page<Player> searchByName(@Param("q") String q, Pageable pageable);

    @Query(
            value =
                    """
                    SELECT p.* FROM players p
                    WHERE (:team IS NULL OR lower(cast(p.team as text)) LIKE lower(concat('%', :team, '%')))
                      AND (:position IS NULL OR lower(cast(p.position as text)) LIKE lower(concat('%', :position, '%')))
                      AND (:q IS NULL OR lower(cast(p.name as text)) LIKE lower(concat('%', :q, '%'))
                           OR lower(cast(p.team as text)) LIKE lower(concat('%', :q, '%'))
                           OR lower(cast(p.league as text)) LIKE lower(concat('%', :q, '%')))
                      AND (:regStart IS NULL OR p.registration_date >= :regStart)
                      AND (:regEnd IS NULL OR p.registration_date <= :regEnd)
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM players p
                    WHERE (:team IS NULL OR lower(cast(p.team as text)) LIKE lower(concat('%', :team, '%')))
                      AND (:position IS NULL OR lower(cast(p.position as text)) LIKE lower(concat('%', :position, '%')))
                      AND (:q IS NULL OR lower(cast(p.name as text)) LIKE lower(concat('%', :q, '%'))
                           OR lower(cast(p.team as text)) LIKE lower(concat('%', :q, '%'))
                           OR lower(cast(p.league as text)) LIKE lower(concat('%', :q, '%')))
                      AND (:regStart IS NULL OR p.registration_date >= :regStart)
                      AND (:regEnd IS NULL OR p.registration_date <= :regEnd)
                    """,
            nativeQuery = true)
    Page<Player> findFiltered(
            @Param("team") String team,
            @Param("position") String position,
            @Param("q") String q,
            @Param("regStart") Instant regStart,
            @Param("regEnd") Instant regEnd,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT * FROM players p
                    WHERE (
                      6378.1 * acos(
                        LEAST(1.0, GREATEST(-1.0,
                          cos(radians(:lat)) * cos(radians((p.location->'coordinates'->>1)::double precision))
                          * cos(radians((p.location->'coordinates'->>0)::double precision) - radians(:lng))
                          + sin(radians(:lat)) * sin(radians((p.location->'coordinates'->>1)::double precision))
                        ))
                      )
                    ) <= :radiusKm
                    ORDER BY cast(p.name as text)
                    """,
            nativeQuery = true)
    List<Player> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);

    @Query(
            value =
                    """
                    SELECT count(*) > 0 FROM players p
                    WHERE lower(cast(p.name as text)) = lower(cast(:name as text))
                      AND lower(cast(p.team as text)) = lower(cast(:team as text))
                    """,
            nativeQuery = true)
    boolean existsByNameIgnoreCaseAndTeamIgnoreCase(@Param("name") String name, @Param("team") String team);

    @Query(
            value =
                    """
                    SELECT count(*) > 0 FROM players p
                    WHERE lower(cast(p.name as text)) = lower(cast(:name as text))
                      AND lower(cast(p.team as text)) = lower(cast(:team as text))
                      AND p.id <> :excludeId
                    """,
            nativeQuery = true)
    boolean existsDuplicateExcluding(
            @Param("name") String name, @Param("team") String team, @Param("excludeId") UUID excludeId);
}
