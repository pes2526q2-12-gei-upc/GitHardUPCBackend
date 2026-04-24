package com.safesteps.backend.domain.users.repository;

import com.safesteps.backend.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR CAST(u.id AS string) LIKE %:query% OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);
}