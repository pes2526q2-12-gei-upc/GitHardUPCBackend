package com.safesteps.backend;

import com.safesteps.backend.domain.chats.dto.*;
import com.safesteps.backend.domain.chats.model.*;
import com.safesteps.backend.domain.chats.repository.*;
import com.safesteps.backend.domain.chats.service.ChatService;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRepository chatRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatParticipantRepository chatParticipantRepository;

    @InjectMocks private ChatService chatService;

    private User userA, userB;
    private Chat groupChat, privateChat;
    private ChatParticipant partA;
    private Chat chatGroup;

    @BeforeEach
    void setUp() {
        userA = new User(); userA.setId(1L); userA.setGoogleId("userA"); userA.setUsername("A");
        userB = new User(); userB.setId(2L); userB.setGoogleId("userB");

        groupChat = new Chat(); groupChat.setId(10L); groupChat.setType("GROUP");
        privateChat = new Chat(); privateChat.setId(11L); privateChat.setType("PRIVATE");

        partA = new ChatParticipant();
        partA.setUser(userA);
        partA.setChat(groupChat);
        partA.setRole("ADMIN");
    }

    @Test
    void createChat_Group_FirstIsAdmin() {
        ChatRequestDTO req = new ChatRequestDTO();
        req.setType("GROUP");
        req.setParticipantGoogleIds(List.of("userA", "userB"));

        when(chatRepository.save(any())).thenReturn(groupChat);
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(userRepository.findByGoogleId("userB")).thenReturn(Optional.of(userB));

        chatService.createChat(req);

        verify(chatParticipantRepository, times(2)).save(any());
    }

    @Test
    void createChat_UserNotFound_ThrowsException() {
        ChatRequestDTO req = new ChatRequestDTO();
        req.setParticipantGoogleIds(List.of("unknown"));
        when(chatRepository.save(any())).thenReturn(groupChat);
        when(userRepository.findByGoogleId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.createChat(req));
    }

    @Test
    void exitGroup_PrivateChat_ThrowsBadRequest() {
        when(chatRepository.findById(11L)).thenReturn(Optional.of(privateChat));
        assertThrows(BadRequestException.class, () -> chatService.exitGroup(11L, "userA"));
    }

    @Test
    void exitGroup_LastParticipant_DeletesChat() {
        when(chatRepository.findById(10L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 1L)).thenReturn(Optional.of(partA));
        when(chatParticipantRepository.countByChatId(10L)).thenReturn(0L);

        chatService.exitGroup(10L, "userA");

        verify(chatRepository).delete(groupChat);
    }

    @Test
    void exitGroup_AdminLeaves_PromotesNext() {
        ChatParticipant partB = new ChatParticipant();
        partB.setUser(userB); partB.setRole(null);

        when(chatRepository.findById(10L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 1L)).thenReturn(Optional.of(partA));
        when(chatParticipantRepository.countByChatId(10L)).thenReturn(1L);
        when(chatParticipantRepository.countByChatIdAndRole(10L, "ADMIN")).thenReturn(0L);
        when(chatParticipantRepository.findByChatIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(partB));

        chatService.exitGroup(10L, "userA");

        assertEquals("ADMIN", partB.getRole());
        verify(chatParticipantRepository).save(partB);
    }

    @Test
    void grantAdmin_Success() {
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 1L)).thenReturn(Optional.of(partA));

        when(userRepository.findByGoogleId("userB")).thenReturn(Optional.of(userB));
        ChatParticipant partB = new ChatParticipant();
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 2L)).thenReturn(Optional.of(partB));

        chatService.grantAdmin(10L, "userA", "userB");

        assertEquals("ADMIN", partB.getRole());
        verify(chatParticipantRepository).save(partB);
    }

    @Test
    void revokeAdmin_LastAdmin_ThrowsException() {
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 1L)).thenReturn(Optional.of(partA));
        when(chatParticipantRepository.countByChatIdAndRole(10L, "ADMIN")).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> chatService.revokeAdmin(10L, "userA", "userA"));
    }

    @Test
    void addUserToGroup_NotAdmin_ThrowsException() {
        ChatParticipant notAdmin = new ChatParticipant();
        notAdmin.setRole(null);
        when(chatRepository.findById(10L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findByGoogleId("userB")).thenReturn(Optional.of(userB));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 2L)).thenReturn(Optional.of(notAdmin));

        assertThrows(BadRequestException.class, () -> chatService.addUserToGroup(10L, "userB", "userA"));
    }

    @Test
    void sendMessage_Success() {
        MessageRequestDTO req = new MessageRequestDTO();
        req.setSenderGoogleId("userA");
        req.setContent("Test");

        when(chatRepository.findById(10L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(messageRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        MessageResponseDTO resp = chatService.sendMessage(10L, req);

        assertNotNull(resp);
        verify(messageRepository).save(any());
    }

    @Test
    void markMessageAsRead_Success() {
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        chatService.markMessageAsRead(100L, "userA");
        verify(messageRepository).markAsRead(100L, 1L);
    }

    @Test
    void testScheduledTasks() {
        when(messageRepository.deleteMessagesReadOlderThan24Hours()).thenReturn(5);
        chatService.deleteOldReadMessages();
        verify(messageRepository).deleteMessagesReadOlderThan24Hours();

        when(chatRepository.deleteChatsWithNoParticipants()).thenReturn(2);
        chatService.deleteEmptyChatsCron();
        verify(chatRepository).deleteChatsWithNoParticipants();
    }

    @Test
    void getUserChats_Success() {
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        when(chatRepository.findChatsByUserId(1L)).thenReturn(new ArrayList<>());

        List<ChatResponseDTO> result = chatService.getUserChats("userA");
        assertNotNull(result);
    }

    @Test
    void revokeAdmin_Success_NotSelf() {
        setupAdminMock("userA", "ADMIN");
        when(userRepository.findByGoogleId("userB")).thenReturn(Optional.of(userB));
        ChatParticipant partB = new ChatParticipant(); partB.setRole("ADMIN");
        when(chatParticipantRepository.findByChatIdAndUserId(10L, userB.getId())).thenReturn(Optional.of(partB));

        chatService.revokeAdmin(10L, "userA", "userB");

        assertNull(partB.getRole());
        verify(chatParticipantRepository).save(partB);
    }

    private void setupAdminMock(String googleId, String role) {
        User u = googleId.equals("userA") ? userA : userB;
        ChatParticipant p = new ChatParticipant(); p.setUser(u); p.setRole(role);
        when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(u));
        when(chatParticipantRepository.findByChatIdAndUserId(10L, u.getId())).thenReturn(Optional.of(p));
    }

    @Test
    void createChat_Private_WrongParticipantCount_ThrowsException() {
        ChatRequestDTO req = new ChatRequestDTO();
        req.setType("PRIVATE");
        req.setParticipantGoogleIds(List.of("userA")); // Només 1 participant en comptes de 2

        assertThrows(BadRequestException.class, () -> chatService.createChat(req));
    }

    @Test
    void createChat_Private_AlreadyExists_ThrowsException() {
        ChatRequestDTO req = new ChatRequestDTO();
        req.setType("PRIVATE");
        req.setParticipantGoogleIds(List.of("userA", "userB"));

        // Simulem que ja existeix un xat privat entre ells
        when(chatRepository.findPrivateChatBetweenUsers("userA", "userB"))
                .thenReturn(Optional.of(privateChat));

        assertThrows(BadRequestException.class, () -> chatService.createChat(req));
    }

    @Test
    void exitGroup_UserNotParticipant_ThrowsException() {
        when(chatRepository.findById(10L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findByGoogleId("userA")).thenReturn(Optional.of(userA));
        // Simulem que l'usuari existeix però no és part d'aquest xat concret
        when(chatParticipantRepository.findByChatIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> chatService.exitGroup(10L, "userA"));
    }

    @Test
    void addUserToGroup_ChatNotGroup_ThrowsException() {
        // Simulem intentar afegir un usuari a un xat privat
        when(chatRepository.findById(11L)).thenReturn(Optional.of(privateChat));

        assertThrows(BadRequestException.class, () -> chatService.addUserToGroup(11L, "userA", "userB"));
    }

    @Test
    void removeUserFromGroup_Success() {
        setupAdminMock("userA", "ADMIN");
        when(userRepository.findByGoogleId("userB")).thenReturn(Optional.of(userB));

        chatService.removeUserFromGroup(10L, "userA", "userB");

        // Verifiquem que crida l'esborrat del repositori
        verify(chatParticipantRepository).deleteByChatIdAndUserId(10L, 2L);
    }

    @Test
    void getMessagesByChatId_Success() {
        Message msg = new Message();
        msg.setId(100L);
        msg.setContent("Test Message");
        msg.setSender(userA);

        when(messageRepository.findByChatIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(msg));

        List<MessageResponseDTO> res = chatService.getMessagesByChatId(10L);

        assertFalse(res.isEmpty());
        assertEquals("Test Message", res.get(0).getContent());
    }

    @Test
    void messageResponse_GetSharedRouteNull() {
        User u = new User();
        u.setGoogleId("userA");
        u.setGoogleId("gId");
        Message msg = new Message();
        msg.setId(100L);
        msg.setSender(u);
        MessageResponseDTO dto = new MessageResponseDTO(msg);
        assertNull(dto.getSharedRoute());
    }

    @Test
    void messageResponse_SenderNull() {
        Message msg = new Message();
        msg.setId(100L);
        msg.setSender(null);
        MessageResponseDTO dto = new MessageResponseDTO(msg);
        assertNull(dto.getSenderUsername());
        assertNull(dto.getSenderGoogleId());
    }

    @Test
    void messageResponse_Null() {
        Message msg = new Message();
        MessageResponseDTO dto = new MessageResponseDTO(msg);
        assertNull(dto.getId());
    }
}