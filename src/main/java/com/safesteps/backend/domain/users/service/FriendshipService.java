package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FriendDTO;
import com.safesteps.backend.domain.users.dto.FriendshipRequestDTO;
import com.safesteps.backend.domain.users.model.Friendship;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.FriendshipRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the accepted friends of a user.
     * The "friend" is whichever side of the friendship is NOT the requesting user.
     */
    @Transactional(readOnly = true)
    public List<FriendDTO> getAcceptedFriends(String googleId) {
        return friendshipRepository
                .findByStatusAndParticipantGoogleId(googleId, FriendshipStatus.ACCEPTED)
                .stream()
                .map(f -> {
                    User friend = f.getSender().getGoogleId().equals(googleId)
                            ? f.getReceiver()
                            : f.getSender();
                    return new FriendDTO(friend);
                })
                .toList();
    }

    /**
     * Returns the pending (inbound) friend requests received by a user.
     * Returns the sender's data so the user knows who sent the request.
     */
    @Transactional(readOnly = true)
    public List<FriendDTO> getPendingRequests(String googleId) {
        return friendshipRepository
                .findByStatusAndReceiverGoogleId(googleId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> new FriendDTO(f.getSender()))
                .toList();
    }

    /**
     * Sends a friend request (creates a PENDING friendship).
     * Validates: no self-request, both users exist, no existing relationship.
     */
    @Transactional
    public void sendFriendRequest(FriendshipRequestDTO req) {
        String senderGoogleId = req.getSenderGoogleId();
        String receiverGoogleId = req.getReceiverGoogleId();

        if (senderGoogleId.equals(receiverGoogleId)) {
            throw new BadRequestException("Cannot send a friend request to yourself.");
        }

        User sender = userRepository.findByGoogleId(senderGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found: " + senderGoogleId));

        User receiver = userRepository.findByGoogleId(receiverGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found: " + receiverGoogleId));

        friendshipRepository.findBetweenUsers(senderGoogleId, receiverGoogleId).ifPresent(f -> {
            throw new BadRequestException("A friendship or pending request already exists between these users.");
        });

        Friendship friendship = new Friendship();
        friendship.setSender(sender);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);
    }

    /**
     * Removes a friendship (accepted or pending) between two users.
     */
    @Transactional
    public void removeFriend(String googleId, String friendGoogleId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(googleId, friendGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No friendship found between users: " + googleId + " and " + friendGoogleId));
        friendshipRepository.delete(friendship);
    }

    /**
     * Accepts a PENDING friend request. Changes its status to ACCEPTED.
     * Only the receiver of the request can accept it.
     */
    @Transactional
    public void acceptFriendRequest(String receiverGoogleId, String senderGoogleId) {
        Friendship friendship = friendshipRepository
                .findByStatusAndSenderAndReceiver(senderGoogleId, receiverGoogleId, FriendshipStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending request found from " + senderGoogleId + " to " + receiverGoogleId));
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    /**
     * Declines a PENDING friend request. Deletes the record from the database.
     * Only the receiver of the request can decline it.
     */
    @Transactional
    public void declineFriendRequest(String receiverGoogleId, String senderGoogleId) {
        Friendship friendship = friendshipRepository
                .findByStatusAndSenderAndReceiver(senderGoogleId, receiverGoogleId, FriendshipStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending request found from " + senderGoogleId + " to " + receiverGoogleId));
        friendshipRepository.delete(friendship);
    }
}
