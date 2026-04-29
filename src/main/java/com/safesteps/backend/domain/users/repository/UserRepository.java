package com.safesteps.backend.domain.users.repository;

import com.safesteps.backend.domain.users.model.Premi;
import com.safesteps.backend.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query(value = """
            SELECT *
            FROM prizes p
            WHERE NOT EXISTS (
                SELECT 1
               FROM user_prizes up
                WHERE up.id = p.id
                  AND up.google_id = :googleId
            )
            """, nativeQuery = true)
    List<Premi> getUserAvailablePrizes(@Param("googleId") String googleId);

    @Modifying
    @Query(value = "INSERT INTO user_prizes (id, google_id) VALUES (:pId, :googleId)", nativeQuery = true)
    void insertUserPrize(@Param("googleId") String googleId, @Param("pId") String pId);

    @Modifying
    @Query(value = "UPDATE users SET pending_rewards = COALESCE(pending_rewards, 0) - 1 WHERE google_id = :googleId AND COALESCE(pending_rewards, 0) > 0", nativeQuery = true)
    int decrementPendingRewards(@Param("googleId") String googleId);
}