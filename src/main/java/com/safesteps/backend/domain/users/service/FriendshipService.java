package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.chats.repository.ChatRepository;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FriendDTO;
import com.safesteps.backend.domain.users.dto.FriendshipRequestDTO;
import com.safesteps.backend.domain.users.model.Friendship;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.FriendshipRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.notifications.NotificationService;
import com.safesteps.backend.notifications.dto.FriendRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatRepository chatRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository, NotificationService notificationService, ChatRepository chatRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.chatRepository = chatRepository;
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
        sendFriendNotification(receiverGoogleId, senderGoogleId, FriendshipStatus.PENDING);
    }

    /**
     * Removes a friendship (accepted or pending) between two users.
     */
    @Transactional
    public void removeFriend(String googleId, String friendGoogleId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(googleId, friendGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No friendship found between users: " + googleId + " and " + friendGoogleId));
        userRepository.deleteEmergencyContacts(friendship.getSender().getGoogleId(), List.of(friendship.getReceiver().getGoogleId()));
        userRepository.deleteEmergencyContacts(friendship.getReceiver().getGoogleId(), List.of(friendship.getSender().getGoogleId()));
        friendshipRepository.delete(friendship);
        chatRepository.findPrivateChatBetweenUsers(googleId, friendGoogleId)
                .ifPresent(chat -> {
                    chatRepository.delete(chat);
                });
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
        sendFriendNotification(senderGoogleId, receiverGoogleId, FriendshipStatus.ACCEPTED);
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
        sendFriendNotification(senderGoogleId, receiverGoogleId, FriendshipStatus.REJECTED);
    }

    // toGoogleId > A qui va la notificacio. fromGoogleId > Qui ha fet l'accio. friendshipStatus > Quina accio s'ha fet (acceptar, rebutjar, pendent)
    private void sendFriendNotification(String toGoogleId, String fromGoogleId, FriendshipStatus friendshipStatus) {
        Optional<User> from = userRepository.findByGoogleId(fromGoogleId);
        if (from.isEmpty()) throw new ResourceNotFoundException("User not found: " + fromGoogleId);
        User u = from.get();
        FriendRequest friendRequest = new FriendRequest(u.getUsername(), friendshipStatus);
        notificationService.sendFriendRequest(toGoogleId, friendRequest);
    }

    public boolean existsFriendship(String googleId, String friendGoogleId) {
        Optional<Friendship> f = friendshipRepository.findBetweenUsers(googleId, friendGoogleId);
        if (f.isEmpty()) return false;
        Friendship friendship = f.get();
        return friendship.getStatus() == FriendshipStatus.ACCEPTED;
    }
}
