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

    @Query("SELECT p FROM Player p WHERE lower(p.name) LIKE lower(concat('%', :q, '%'))")
    Page<Player> searchByName(@Param("q") String q, Pageable pageable);

    @Query(
            """
            SELECT p FROM Player p
            WHERE (:team IS NULL OR lower(p.team) LIKE lower(concat('%', :team, '%')))
              AND (:position IS NULL OR lower(p.position) LIKE lower(concat('%', :position, '%')))
              AND (:q IS NULL OR lower(p.name) LIKE lower(concat('%', :q, '%'))
                   OR lower(p.team) LIKE lower(concat('%', :q, '%'))
                   OR lower(p.league) LIKE lower(concat('%', :q, '%')))
              AND (:regStart IS NULL OR p.registrationDate >= :regStart)
              AND (:regEnd IS NULL OR p.registrationDate <= :regEnd)
            """)
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
                    ORDER BY p.name
                    """,
            nativeQuery = true)
    List<Player> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);

    @Query(
            """
            SELECT COUNT(p) > 0 FROM Player p
            WHERE lower(p.name) = lower(:name) AND lower(p.team) = lower(:team)
            """)
    boolean existsByNameIgnoreCaseAndTeamIgnoreCase(@Param("name") String name, @Param("team") String team);

    @Query(
            """
            SELECT COUNT(p) > 0 FROM Player p
            WHERE lower(p.name) = lower(:name) AND lower(p.team) = lower(:team) AND p.id <> :excludeId
            """)
    boolean existsDuplicateExcluding(
            @Param("name") String name, @Param("team") String team, @Param("excludeId") UUID excludeId);
}
