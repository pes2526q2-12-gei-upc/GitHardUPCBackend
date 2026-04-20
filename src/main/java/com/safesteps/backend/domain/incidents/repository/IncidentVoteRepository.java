package com.safesteps.backend.domain.incidents.repository;

import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentVoteRepository extends JpaRepository<Vote, Long>  {


    @Query(value = """
            SELECT *
            FROM votes
            WHERE incidence_id = :incidenceId AND user_id = :userId
            """, nativeQuery = true)
    Optional<Vote> findByUserAndIncidence(@Param("incidenceId") Long incidenceId, @Param("userId") Long userId);

    @Query(value = """
            SELECT 
                id AS id,
                incidence_id AS incidenceId,
                user_id AS userId,
                score AS score,
                created_at AS createdAt
            FROM votes
            WHERE user_id = :userId
            """, nativeQuery = true)
    List<VoteDBProjection> findAllByUserId(@Param("userId") Long userId);

}
