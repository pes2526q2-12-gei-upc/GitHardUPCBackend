package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FiltreRepository extends JpaRepository<Carrer, Long> {
    @Query(value = "SELECT * FROM filtres WHERE google_id = :googleId", nativeQuery = true)
    FiltreDBProjection findByGoogleId(String googleId);
}
