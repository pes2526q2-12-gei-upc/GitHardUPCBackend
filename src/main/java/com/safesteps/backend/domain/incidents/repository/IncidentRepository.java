package com.safesteps.backend.domain.incidents.repository;

import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @Query(value = """
            SELECT 
                i.id AS id, 
                u.username AS username, 
                u.level AS userLevel, 
                i.type AS type, 
                i.description AS description, 
                i.location AS location, 
                i.positive_votes AS positiveVotes, 
                i.negative_votes AS negativeVotes, 
                i.reliability_index AS reliabilityIndex, 
                i.status AS status, 
                i.created_at AS created, 
                i.updated_at AS updated
            FROM incidents i
            JOIN users u ON i.user_id = u.id
            ORDER BY i.id
            """, nativeQuery = true)
    List<IncidentDBProjection> all();

    @Query(value = """
            SELECT 
                i.id AS id, 
                u.username AS username, 
                u.level AS userLevel, 
                i.type AS type, 
                i.description AS description, 
                i.location AS location, 
                i.positive_votes AS positiveVotes, 
                i.negative_votes AS negativeVotes, 
                i.reliability_index AS reliabilityIndex, 
                i.status AS status, 
                i.created_at AS created, 
                i.updated_at AS updated
            FROM incidents i
            JOIN users u ON i.user_id = u.id
            WHERE i.id = :id
            """, nativeQuery = true)
    Optional<IncidentDBProjection> findIncidentWithUserById(@Param("id") Long id);

    @Query(value = """
            SELECT 
                i.id AS id, 
                u.username AS username, 
                u.level AS userLevel, 
                i.type AS type, 
                i.description AS description, 
                i.location AS location, 
                i.positive_votes AS positiveVotes, 
                i.negative_votes AS negativeVotes, 
                i.reliability_index AS reliabilityIndex, 
                i.status AS status, 
                i.created_at AS created, 
                i.updated_at AS updated
            FROM incidents i
            JOIN users u ON i.user_id = u.id
            WHERE i.user_id = :userId
            ORDER BY i.id
            """, nativeQuery = true)
    List<IncidentDBProjection> getAllByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT 
                i.id as id,
                i.positive_votes as positiveVotes,
                i.negative_votes as negativeVotes,
                i.reliability_index as reliabilityIndex
            FROM incidents i
            WHERE i.id = :id
            """, nativeQuery = true)
    Optional<VoteCountDBProjection> getVoteCount(@Param("id") Long id);
}
