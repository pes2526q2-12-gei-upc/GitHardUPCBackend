package com.safesteps.backend.domain.users.repository;

import com.safesteps.backend.domain.users.model.Friendship;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Returns all friendships of a given status where the user is sender OR receiver.
     * Used for ACCEPTED to get the full friends list.
     */
    @Query("SELECT f FROM Friendship f WHERE f.status = :status " +
           "AND (f.sender.googleId = :googleId OR f.receiver.googleId = :googleId)")
    List<Friendship> findByStatusAndParticipantGoogleId(
            @Param("googleId") String googleId,
            @Param("status") FriendshipStatus status);

    /**
     * Returns all friendships of a given status where the user is the receiver.
     * Used for PENDING to get inbound requests.
     */
    @Query("SELECT f FROM Friendship f WHERE f.status = :status " +
           "AND f.receiver.googleId = :googleId")
    List<Friendship> findByStatusAndReceiverGoogleId(
            @Param("googleId") String googleId,
            @Param("status") FriendshipStatus status);

    /**
     * Finds any friendship between two users regardless of direction.
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.sender.googleId = :g1 AND f.receiver.googleId = :g2) OR " +
           "(f.sender.googleId = :g2 AND f.receiver.googleId = :g1)")
    Optional<Friendship> findBetweenUsers(@Param("g1") String g1, @Param("g2") String g2);

    /**
     * Finds a friendship with a specific status in a specific direction.
     * Used for accept/decline: only the receiver can act on a PENDING request.
     */
    @Query("SELECT f FROM Friendship f WHERE f.status = :status " +
           "AND f.sender.googleId = :senderGoogleId " +
           "AND f.receiver.googleId = :receiverGoogleId")
    Optional<Friendship> findByStatusAndSenderAndReceiver(
            @Param("senderGoogleId") String senderGoogleId,
            @Param("receiverGoogleId") String receiverGoogleId,
            @Param("status") FriendshipStatus status);
}
