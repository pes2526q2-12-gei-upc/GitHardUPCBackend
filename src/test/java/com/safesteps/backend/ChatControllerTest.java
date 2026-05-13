package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.ChatController;
import com.safesteps.backend.domain.admin.service.AdminMetricsService; // Faltava aquesta importació
import com.safesteps.backend.domain.chats.dto.*;
import com.safesteps.backend.domain.chats.service.ChatService;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.GlobalExceptionHandler;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ChatService chatService;

    @MockBean private AdminMetricsService adminMetricsService;

    @Test
    void getUserChats_ShouldReturnOk() throws Exception {
        when(chatService.getUserChats("userA")).thenReturn(List.of(new ChatResponseDTO()));
        mockMvc.perform(get("/api/v1/chats/user/userA"))
                .andExpect(status().isOk());
    }

    @Test
    void createChat_ShouldReturnCreated() throws Exception {
        ChatRequestDTO req = new ChatRequestDTO();
        req.setType("GROUP");
        req.setParticipantGoogleIds(List.of("userA"));

        when(chatService.createChat(any())).thenReturn(new ChatResponseDTO());

        mockMvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void addUser_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/chats/1/participants")
                        .param("adminId", "admin")
                        .param("newUserGoogleId", "new"))
                .andExpect(status().isOk());
        verify(chatService).addUserToGroup(1L, "admin", "new");
    }

    @Test
    void exitGroup_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chats/1/participants/exit")
                        .param("googleId", "userA"))
                .andExpect(status().isNoContent());
        verify(chatService).exitGroup(1L, "userA");
    }

    @Test
    void removeUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chats/1/participants/targetA")
                        .param("adminId", "admin"))
                .andExpect(status().isNoContent());
        verify(chatService).removeUserFromGroup(1L, "admin", "targetA");
    }

    @Test
    void grantAdmin_ShouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/v1/chats/1/participants/targetA/admin")
                        .param("adminId", "admin"))
                .andExpect(status().isOk());
        verify(chatService).grantAdmin(1L, "admin", "targetA");
    }

    @Test
    void revokeAdmin_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chats/1/participants/targetA/admin")
                        .param("adminId", "admin"))
                .andExpect(status().isNoContent());
        verify(chatService).revokeAdmin(1L, "admin", "targetA");
    }

    @Test
    void sendMessage_ShouldReturnCreated() throws Exception {
        MessageRequestDTO req = new MessageRequestDTO();
        req.setContent("Hola");
        req.setSenderGoogleId("userA");

        when(chatService.sendMessage(eq(1L), any())).thenReturn(new MessageResponseDTO());

        mockMvc.perform(post("/api/v1/chats/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void getMessages_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/chats/1/messages"))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/chats/1/messages/100/read")
                        .param("googleId", "userA"))
                .andExpect(status().isNoContent());
        verify(chatService).markMessageAsRead(100L, "userA");
    }

    @Test
    void getUserChats_NotFound() throws Exception {
        when(chatService.getUserChats("unknown")).thenThrow(new ResourceNotFoundException("Usuari no trobat"));

        mockMvc.perform(get("/api/v1/chats/user/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createChat_ValidationErrorReported() throws Exception {
        mockMvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantAdmin_BadRequest() throws Exception {
        doThrow(new BadRequestException("L'usuari ja és admin"))
                .when(chatService).grantAdmin(anyLong(), anyString(), anyString());

        mockMvc.perform(patch("/api/v1/chats/1/participants/userB/admin")
                        .param("adminId", "userA"))
                .andExpect(status().isBadRequest());
    }
}