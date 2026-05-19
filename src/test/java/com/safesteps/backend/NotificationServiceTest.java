package com.safesteps.backend;

import com.safesteps.backend.domain.chats.dto.MessageResponseDTO;
import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.notifications.NotificationService;
import com.safesteps.backend.notifications.dto.EmergencyLocation;
import com.safesteps.backend.notifications.dto.FriendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private NotificationService notificationService;

    private List<String> googleIds;
    private User user;

    @BeforeEach
    void setUp() {
        googleIds = new ArrayList<>();
        googleIds.add("gId1");
        String googleId = "googleId";
        user = new User();
        user.setGoogleId(googleId);
        user.setIsOnline(true);
        user.setUsername("username");
        user.setFcmToken("token_firebase");
    }

    @Test
    void send_ExcInWS() {
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        doThrow(new RuntimeException("Socket closed")).when(messagingTemplate)
                .convertAndSendToUser(any(), any(), any());

        notificationService.sendEmergency(googleIds, true, user.getUsername());

        verify(messagingTemplate).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_GoogleIdNull() {
        List<String> list = new ArrayList<>();
        list.add(null);

        notificationService.sendEmergency(list, true, user.getUsername());

        verify(userRepository, never()).findByGoogleId(any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_UserNotFound() {
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.empty());

        notificationService.sendEmergency(googleIds, true, user.getUsername());

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_UserOffline() {
        user.setIsOnline(false);
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        notificationService.sendEmergency(googleIds, true, user.getUsername());

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_UserNullOnline() {
        user.setIsOnline(null);
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        notificationService.sendEmergency(googleIds, true, user.getUsername());

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_NullEmergencyContacts() {
        notificationService.sendEmergency(List.of(), true, user.getUsername());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_Ok() {
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        notificationService.sendEmergency(googleIds, true, user.getUsername());

        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_OkReturnNormality() {
        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        notificationService.sendEmergency(googleIds, false, user.getUsername());

        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_Location() {
        Coord coord = new Coord();
        EmergencyLocation loc = new EmergencyLocation(user.getUsername(), coord);

        notificationService.sendLocationUpdate(googleIds, loc);

        verify(messagingTemplate, times(googleIds.size())).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_LocationNoneContacts() {
        EmergencyLocation loc = mock(EmergencyLocation.class);

        notificationService.sendLocationUpdate(List.of(), loc);

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_Message() {
        MessageResponseDTO responseDTO = new MessageResponseDTO();

        when(userRepository.findByGoogleId(googleIds.getFirst())).thenReturn(Optional.of(user));

        notificationService.sendMessage(googleIds, responseDTO);

        verify(messagingTemplate, times(googleIds.size())).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void send_FriendRequest_Pending() {
        FriendRequest fr = new FriendRequest(user.getGoogleId(), FriendshipStatus.PENDING );
        String receiverId = googleIds.getFirst();
        User receiver = new User();
        receiver.setGoogleId(receiverId);
        receiver.setIsOnline(true);

        when(userRepository.findByGoogleId(receiverId)).thenReturn(Optional.of(receiver));

        notificationService.sendFriendRequest(receiverId, fr);

        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("data", fr);
        expectedPayload.put("titleKey", "FRIEND_REQ_TITLE");
        expectedPayload.put("bodyKey", "FRIEND_REQ_BODY");

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq(receiverId), eq("/queue/requests"), eq(expectedPayload));
    }

    @Test
    void send_FriendRequest_Accepted() {
        FriendRequest fr = new FriendRequest(user.getGoogleId(), FriendshipStatus.ACCEPTED );
        String receiverId = googleIds.getFirst();
        User receiver = new User();
        receiver.setGoogleId(receiverId);
        receiver.setIsOnline(true);

        when(userRepository.findByGoogleId(receiverId)).thenReturn(Optional.of(receiver));

        notificationService.sendFriendRequest(receiverId, fr);

        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("data", fr);
        expectedPayload.put("titleKey", "FRIEND_ACC_TITLE");
        expectedPayload.put("bodyKey", "FRIEND_ACC_BODY");

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq(receiverId), eq("/queue/requests"), eq(expectedPayload));
    }

    @Test
    void send_FriendRequest_Rejected() {
        FriendRequest fr = new FriendRequest(user.getGoogleId(), FriendshipStatus.REJECTED );
        String receiverId = googleIds.getFirst();
        User receiver = new User();
        receiver.setGoogleId(receiverId);
        receiver.setIsOnline(true);

        when(userRepository.findByGoogleId(receiverId)).thenReturn(Optional.of(receiver));

        notificationService.sendFriendRequest(receiverId, fr);

        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("data", fr);
        expectedPayload.put("titleKey", "FRIEND_REJ_TITLE");
        expectedPayload.put("bodyKey", "FRIEND_REJ_BODY");

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq(receiverId), eq("/queue/requests"), eq(expectedPayload));
    }

}