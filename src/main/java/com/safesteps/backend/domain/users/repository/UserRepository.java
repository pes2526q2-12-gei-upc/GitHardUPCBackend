package com.safesteps.backend.domain.users.repository;

import com.safesteps.backend.domain.users.model.Premi;
import com.safesteps.backend.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "premis")
    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String username);

    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR CAST(u.id AS string) LIKE %:query% OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);

    @Query("""
    SELECT p FROM Premi p
    WHERE p.id NOT IN (
        SELECT up.id FROM User u JOIN u.premis up WHERE u.googleId = :googleId
    )
""")
    List<Premi> getUserAvailablePrizes(@Param("googleId") String googleId);

    @Modifying
    @Query(value = "INSERT INTO user_prizes (id, google_id) VALUES (:pId, :googleId)", nativeQuery = true)
    void insertUserPrize(@Param("googleId") String googleId, @Param("pId") String pId);

    @Modifying
    @Query(value = "UPDATE users SET pending_rewards = COALESCE(pending_rewards, 0) - 1 WHERE google_id = :googleId AND COALESCE(pending_rewards, 0) > 0", nativeQuery = true)
    int decrementPendingRewards(@Param("googleId") String googleId);

    @Modifying
    @Query(value = "INSERT INTO emergency_contacts (user_google_id, emergency_contact_google_id) VALUES (:userGoogleId, :emergencyContactGoogleId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addEmergencyContact(@Param("userGoogleId") String userGoogleId, @Param("emergencyContactGoogleId") String emergencyContactGoogleId);

    @Modifying
    @Query(value = "DELETE FROM emergency_contacts WHERE user_google_id = :userGoogleId AND emergency_contact_google_id IN (:emergencyContactsGoogleIds)", nativeQuery = true)
    void deleteEmergencyContacts(@Param("userGoogleId") String userGoogleId, @Param("emergencyContactsGoogleId") List<String> emergencyContactsGoogleId);

    @Query(value = "SELECT emergency_contact_google_id FROM emergency_contacts WHERE user_google_id = :userGoogleId", nativeQuery = true)
    List<String> getEmergencyContacts(@Param("userGoogleId") String userGoogleId);

    List<User> findByGoogleIdIn(List<String> googleIds);
}