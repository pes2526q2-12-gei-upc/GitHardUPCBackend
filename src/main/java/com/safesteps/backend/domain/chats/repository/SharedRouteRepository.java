package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.SharedRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedRouteRepository extends JpaRepository<SharedRoute, Long> {
}
