package com.safesteps.backend.domain.incidents.repository;

import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentVoteRepository extends JpaRepository<Vote, Long>  {


    @Query(value = """
            SELECT *
            FROM votes
            WHERE incidence_id = :incidenceId AND google_id = :googleId
            """, nativeQuery = true)
    Optional<Vote> findByUserAndIncidence(@Param("incidenceId") Long incidenceId, @Param("googleId") String googleId);

    @Query(value = """
            SELECT
                id AS id,
                incidence_id AS incidenceId,
                google_id AS googleId,
                score AS score,
                created_at AS createdAt
            FROM votes
            WHERE google_id = :googleId
            """, nativeQuery = true)
    List<VoteDBProjection> findAllByGoogleId(@Param("googleId") String googleId);

    @Query(value = """
            SELECT
                id AS id,
                incidence_id AS incidenceId,
                google_id AS googleId,
                score AS score,
                created_at AS createdAt
            FROM votes
            WHERE incidence_id = :incidenceId
            """, nativeQuery = true)
    List<VoteDBProjection> findAllByIncidenceId(@Param("incidenceId") Long incidenceId);

    @Modifying
    @Query(value = "DELETE FROM votes WHERE incidence_id IN (SELECT id FROM incidents WHERE created_at < :dateLimit)", nativeQuery = true)
    void deleteVotesFromOldIncidents(@Param("dateLimit") OffsetDateTime dateLimit);

}
