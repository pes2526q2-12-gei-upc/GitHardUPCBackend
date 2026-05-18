package com.safesteps.backend;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FriendDTO;
import com.safesteps.backend.domain.users.dto.FriendshipRequestDTO;
import com.safesteps.backend.domain.users.model.Friendship;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.FriendshipRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.users.service.FriendshipService;
import com.safesteps.backend.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FriendshipService friendshipService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setGoogleId("googleA");
        userA.setUsername("userA");
        userA.setEmail("a@test.com");
        userA.setPictureUrl("urlA");

        userB = new User();
        userB.setGoogleId("googleB");
        userB.setUsername("userB");
        userB.setEmail("b@test.com");
        userB.setPictureUrl("urlB");
    }

    // ---- getAcceptedFriends ----

    @Test
    @DisplayName("getAcceptedFriends: devuelve el otro extremo de la amistad cuando el usuario es sender")
    void getAcceptedFriends_WhenUserIsSender_ReturnsFriendAsFriendDTO() {
        Friendship f = new Friendship();
        f.setSender(userA);
        f.setReceiver(userB);
        f.setStatus(FriendshipStatus.ACCEPTED);

        when(friendshipRepository.findByStatusAndParticipantGoogleId("googleA", FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(f));

        List<FriendDTO> result = friendshipService.getAcceptedFriends("googleA");

        assertEquals(1, result.size());
        assertEquals("googleB", result.get(0).getGoogleId());
        assertEquals("userB", result.get(0).getUsername());
    }

    @Test
    @DisplayName("getAcceptedFriends: devuelve el otro extremo de la amistad cuando el usuario es receiver")
    void getAcceptedFriends_WhenUserIsReceiver_ReturnsSenderAsFriendDTO() {
        Friendship f = new Friendship();
        f.setSender(userB);
        f.setReceiver(userA);
        f.setStatus(FriendshipStatus.ACCEPTED);

        when(friendshipRepository.findByStatusAndParticipantGoogleId("googleA", FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(f));

        List<FriendDTO> result = friendshipService.getAcceptedFriends("googleA");

        assertEquals(1, result.size());
        assertEquals("googleB", result.get(0).getGoogleId());
    }

    @Test
    @DisplayName("getAcceptedFriends: devuelve lista vacía si no tiene amigos")
    void getAcceptedFriends_WhenNoFriends_ReturnsEmptyList() {
        when(friendshipRepository.findByStatusAndParticipantGoogleId("googleA", FriendshipStatus.ACCEPTED))
                .thenReturn(List.of());

        List<FriendDTO> result = friendshipService.getAcceptedFriends("googleA");

        assertTrue(result.isEmpty());
    }

    // ---- getPendingRequests ----

    @Test
    @DisplayName("getPendingRequests: devuelve las solicitudes recibidas con los datos del remitente")
    void getPendingRequests_WhenHasPending_ReturnsSenders() {
        Friendship f = new Friendship();
        f.setSender(userB);
        f.setReceiver(userA);
        f.setStatus(FriendshipStatus.PENDING);

        when(friendshipRepository.findByStatusAndReceiverGoogleId("googleA", FriendshipStatus.PENDING))
                .thenReturn(List.of(f));

        List<FriendDTO> result = friendshipService.getPendingRequests("googleA");

        assertEquals(1, result.size());
        assertEquals("googleB", result.get(0).getGoogleId());
        assertEquals("userB", result.get(0).getUsername());
    }

    @Test
    @DisplayName("getPendingRequests: devuelve lista vacía si no tiene solicitudes")
    void getPendingRequests_WhenNoPending_ReturnsEmptyList() {
        when(friendshipRepository.findByStatusAndReceiverGoogleId("googleA", FriendshipStatus.PENDING))
                .thenReturn(List.of());

        List<FriendDTO> result = friendshipService.getPendingRequests("googleA");

        assertTrue(result.isEmpty());
    }

    // ---- sendFriendRequest ----

    @Test
    @DisplayName("sendFriendRequest: crea amistad PENDING correctamente")
    void sendFriendRequest_WhenValid_SavesFriendship() {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        when(userRepository.findByGoogleId("googleA")).thenReturn(Optional.of(userA));
        when(userRepository.findByGoogleId("googleB")).thenReturn(Optional.of(userB));
        when(friendshipRepository.findBetweenUsers("googleA", "googleB")).thenReturn(Optional.empty());

        friendshipService.sendFriendRequest(req);

        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @Test
    @DisplayName("sendFriendRequest: lanza BadRequest si el sender y receiver son el mismo")
    void sendFriendRequest_WhenSelfRequest_ThrowsBadRequest() {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleA");

        assertThrows(BadRequestException.class, () -> friendshipService.sendFriendRequest(req));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendFriendRequest: lanza ResourceNotFound si el sender no existe")
    void sendFriendRequest_WhenSenderNotFound_ThrowsResourceNotFound() {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        when(userRepository.findByGoogleId("googleA")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> friendshipService.sendFriendRequest(req));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendFriendRequest: lanza ResourceNotFound si el receiver no existe")
    void sendFriendRequest_WhenReceiverNotFound_ThrowsResourceNotFound() {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        when(userRepository.findByGoogleId("googleA")).thenReturn(Optional.of(userA));
        when(userRepository.findByGoogleId("googleB")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> friendshipService.sendFriendRequest(req));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendFriendRequest: lanza BadRequest si ya existe una amistad entre los dos usuarios")
    void sendFriendRequest_WhenAlreadyExists_ThrowsBadRequest() {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        when(userRepository.findByGoogleId("googleA")).thenReturn(Optional.of(userA));
        when(userRepository.findByGoogleId("googleB")).thenReturn(Optional.of(userB));
        when(friendshipRepository.findBetweenUsers("googleA", "googleB"))
                .thenReturn(Optional.of(new Friendship()));

        assertThrows(BadRequestException.class, () -> friendshipService.sendFriendRequest(req));
        verify(friendshipRepository, never()).save(any());
    }

    // ---- removeFriend ----

    @Test
    @DisplayName("removeFriend: elimina la amistad correctamente")
    void removeFriend_WhenExists_DeletesFriendship() {
        Friendship f = new Friendship();
        when(friendshipRepository.findBetweenUsers("googleA", "googleB")).thenReturn(Optional.of(f));

        friendshipService.removeFriend("googleA", "googleB");

        verify(friendshipRepository, times(1)).delete(f);
    }

    @Test
    @DisplayName("removeFriend: lanza ResourceNotFound si no existe la amistad")
    void removeFriend_WhenNotExists_ThrowsResourceNotFound() {
        when(friendshipRepository.findBetweenUsers("googleA", "googleB")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> friendshipService.removeFriend("googleA", "googleB"));
        verify(friendshipRepository, never()).delete(any());
    }

    // ---- acceptFriendRequest ----

    @Test
    @DisplayName("acceptFriendRequest: cambia el estado a ACCEPTED correctamente")
    void acceptFriendRequest_WhenValid_SetsStatusAccepted() {
        Friendship f = new Friendship();
        f.setSender(userB);
        f.setReceiver(userA);
        f.setStatus(FriendshipStatus.PENDING);
        User u = new User();
        u.setUsername("us");

        when(friendshipRepository.findByStatusAndSenderAndReceiver("googleB", "googleA", FriendshipStatus.PENDING))
                .thenReturn(Optional.of(f));
        when (userRepository.findByGoogleId("googleB")).thenReturn(Optional.of(u));

        friendshipService.acceptFriendRequest("googleA", "googleB");

        assertEquals(FriendshipStatus.ACCEPTED, f.getStatus());
        verify(friendshipRepository, times(1)).save(f);
    }

    @Test
    @DisplayName("acceptFriendRequest: lanza ResourceNotFound si no existe la solicitud PENDING")
    void acceptFriendRequest_WhenNotFound_ThrowsResourceNotFound() {
        when(friendshipRepository.findByStatusAndSenderAndReceiver("googleB", "googleA", FriendshipStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> friendshipService.acceptFriendRequest("googleA", "googleB"));
        verify(friendshipRepository, never()).save(any());
    }

    // ---- declineFriendRequest ----

    @Test
    @DisplayName("declineFriendRequest: elimina el registro PENDING correctamente")
    void declineFriendRequest_WhenValid_DeletesFriendship() {
        Friendship f = new Friendship();
        f.setSender(userB);
        f.setReceiver(userA);
        f.setStatus(FriendshipStatus.PENDING);
        User u = new User();
        u.setUsername("us");

        when(friendshipRepository.findByStatusAndSenderAndReceiver("googleB", "googleA", FriendshipStatus.PENDING))
                .thenReturn(Optional.of(f));
        when (userRepository.findByGoogleId("googleB")).thenReturn(Optional.of(u));


        friendshipService.declineFriendRequest("googleA", "googleB");

        verify(friendshipRepository, times(1)).delete(f);
    }

    @Test
    @DisplayName("declineFriendRequest: lanza ResourceNotFound si no existe la solicitud PENDING")
    void declineFriendRequest_WhenNotFound_ThrowsResourceNotFound() {
        when(friendshipRepository.findByStatusAndSenderAndReceiver("googleB", "googleA", FriendshipStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> friendshipService.declineFriendRequest("googleA", "googleB"));
        verify(friendshipRepository, never()).delete(any());
    }
}
