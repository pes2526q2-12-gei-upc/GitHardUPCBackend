package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.FriendshipController;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.GlobalExceptionHandler;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FriendDTO;
import com.safesteps.backend.domain.users.dto.FriendshipRequestDTO;
import com.safesteps.backend.domain.users.service.FriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FriendshipControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FriendshipService friendshipService;

    @InjectMocks
    private FriendshipController friendshipController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(friendshipController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- GET /{googleId}/accepted ----

    @Test
    @DisplayName("GET /api/v1/friendships/{googleId}/accepted - Devuelve lista de amigos (200)")
    void getAcceptedFriends_ReturnsOk() throws Exception {
        FriendDTO friend = new FriendDTO();
        friend.setGoogleId("googleB");
        friend.setUsername("userB");
        friend.setEmail("b@test.com");
        friend.setPictureUrl("urlB");

        when(friendshipService.getAcceptedFriends("googleA")).thenReturn(List.of(friend));

        mockMvc.perform(get("/api/v1/friendships/googleA/accepted"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].googleId").value("googleB"))
                .andExpect(jsonPath("$[0].username").value("userB"))
                .andExpect(jsonPath("$[0].email").value("b@test.com"));
    }

    @Test
    @DisplayName("GET /api/v1/friendships/{googleId}/accepted - Lista vacía (200)")
    void getAcceptedFriends_EmptyList_ReturnsOk() throws Exception {
        when(friendshipService.getAcceptedFriends("googleA")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/friendships/googleA/accepted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /{googleId}/pending ----

    @Test
    @DisplayName("GET /api/v1/friendships/{googleId}/pending - Devuelve solicitudes pendientes (200)")
    void getPendingRequests_ReturnsOk() throws Exception {
        FriendDTO sender = new FriendDTO();
        sender.setGoogleId("googleB");
        sender.setUsername("userB");
        sender.setEmail("b@test.com");

        when(friendshipService.getPendingRequests("googleA")).thenReturn(List.of(sender));

        mockMvc.perform(get("/api/v1/friendships/googleA/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].googleId").value("googleB"));
    }

    @Test
    @DisplayName("GET /api/v1/friendships/{googleId}/pending - Sin solicitudes (200 vacío)")
    void getPendingRequests_EmptyList_ReturnsOk() throws Exception {
        when(friendshipService.getPendingRequests("googleA")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/friendships/googleA/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- POST / ----

    @Test
    @DisplayName("POST /api/v1/friendships - Solicitud creada correctamente (201)")
    void sendFriendRequest_ReturnsCreated() throws Exception {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        doNothing().when(friendshipService).sendFriendRequest(any(FriendshipRequestDTO.class));

        mockMvc.perform(post("/api/v1/friendships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/friendships - Amistad ya existe (400)")
    void sendFriendRequest_WhenAlreadyExists_ReturnsBadRequest() throws Exception {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleB");

        doThrow(new BadRequestException("A friendship or pending request already exists between these users."))
                .when(friendshipService).sendFriendRequest(any(FriendshipRequestDTO.class));

        mockMvc.perform(post("/api/v1/friendships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/friendships - Usuario no encontrado (404)")
    void sendFriendRequest_WhenUserNotFound_ReturnsNotFound() throws Exception {
        FriendshipRequestDTO req = new FriendshipRequestDTO();
        req.setSenderGoogleId("googleA");
        req.setReceiverGoogleId("googleX");

        doThrow(new ResourceNotFoundException("Receiver not found: googleX"))
                .when(friendshipService).sendFriendRequest(any(FriendshipRequestDTO.class));

        mockMvc.perform(post("/api/v1/friendships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /{googleId}/friend/{friendGoogleId} ----

    @Test
    @DisplayName("DELETE /api/v1/friendships/{googleId}/friend/{friendGoogleId} - Elimina correctamente (204)")
    void removeFriend_ReturnsNoContent() throws Exception {
        doNothing().when(friendshipService).removeFriend(eq("googleA"), eq("googleB"));

        mockMvc.perform(delete("/api/v1/friendships/googleA/friend/googleB"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/friendships/{googleId}/friend/{friendGoogleId} - No existe la amistad (404)")
    void removeFriend_WhenNotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("No friendship found between these users."))
                .when(friendshipService).removeFriend(eq("googleA"), eq("googleB"));

        mockMvc.perform(delete("/api/v1/friendships/googleA/friend/googleB"))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH /{receiverGoogleId}/accept/{senderGoogleId} ----

    @Test
    @DisplayName("PATCH /api/v1/friendships/{receiverGoogleId}/accept/{senderGoogleId} - Acepta correctamente (200)")
    void acceptFriendRequest_ReturnsOk() throws Exception {
        doNothing().when(friendshipService).acceptFriendRequest(eq("googleA"), eq("googleB"));

        mockMvc.perform(patch("/api/v1/friendships/googleA/accept/googleB"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/friendships/{receiverGoogleId}/accept/{senderGoogleId} - No existe la solicitud (404)")
    void acceptFriendRequest_WhenNotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("No pending request found from googleB to googleA"))
                .when(friendshipService).acceptFriendRequest(eq("googleA"), eq("googleB"));

        mockMvc.perform(patch("/api/v1/friendships/googleA/accept/googleB"))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /{receiverGoogleId}/decline/{senderGoogleId} ----

    @Test
    @DisplayName("DELETE /api/v1/friendships/{receiverGoogleId}/decline/{senderGoogleId} - Deniega correctamente (204)")
    void declineFriendRequest_ReturnsNoContent() throws Exception {
        doNothing().when(friendshipService).declineFriendRequest(eq("googleA"), eq("googleB"));

        mockMvc.perform(delete("/api/v1/friendships/googleA/decline/googleB"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/friendships/{receiverGoogleId}/decline/{senderGoogleId} - No existe la solicitud (404)")
    void declineFriendRequest_WhenNotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("No pending request found from googleB to googleA"))
                .when(friendshipService).declineFriendRequest(eq("googleA"), eq("googleB"));

        mockMvc.perform(delete("/api/v1/friendships/googleA/decline/googleB"))
                .andExpect(status().isNotFound());
    }
}
