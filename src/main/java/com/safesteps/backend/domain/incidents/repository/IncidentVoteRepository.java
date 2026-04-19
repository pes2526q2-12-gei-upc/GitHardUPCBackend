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
            WHERE id_incidence = :incidenceId AND id_user = :userId
            """, nativeQuery = true)
    Optional<Vote> findByUserAndIncidence(@Param("incidenceId") Long incidenceId, @Param("userId") Long userId);

    @Query(value = """
            SELECT 
                id AS id,
                id_incidence AS idIncidence,
                id_user AS idUser,
                score AS score,
                created_at AS createdAt
            FROM votes
            WHERE id_user = :userId
            """, nativeQuery = true)
    List<VoteDBProjection> findAllByUserId(@Param("userId") Long userId);

}
