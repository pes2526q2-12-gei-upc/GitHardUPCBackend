package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FiltreRepository extends JpaRepository<Filtre, String> {
    @Query(value = "SELECT * FROM filtres WHERE google_id = :googleId", nativeQuery = true)
    FiltreDBProjection findByGoogleId(@Param("googleId") String googleId);
}
