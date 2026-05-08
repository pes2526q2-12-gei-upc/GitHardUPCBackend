package com.safesteps.backend.domain.chats.service;

import com.safesteps.backend.domain.chats.dto.ChatRequestDTO;
import com.safesteps.backend.domain.chats.dto.ChatResponseDTO;
import com.safesteps.backend.domain.chats.dto.MessageRequestDTO;
import com.safesteps.backend.domain.chats.dto.MessageResponseDTO;
import com.safesteps.backend.domain.chats.model.Chat;
import com.safesteps.backend.domain.chats.model.ChatParticipant;
import com.safesteps.backend.domain.chats.model.Message;
import com.safesteps.backend.domain.chats.repository.ChatRepository;
import com.safesteps.backend.domain.chats.repository.MessageRepository;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatResponseDTO createChat(ChatRequestDTO req) {
        Chat chat = new Chat();
        chat.setType(req.getType());
        chat.setName(req.getName());

        for (String googleId : req.getParticipantGoogleIds()) {
            User user = userRepository.findByGoogleId(googleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuari amb Google ID " + googleId + " no trobat"));

            ChatParticipant participant = new ChatParticipant();
            participant.setChat(chat);
            participant.setUser(user);
            chat.getParticipants().add(participant);
        }

        Chat savedChat = chatRepository.save(chat);
        return new ChatResponseDTO(savedChat);
    }

    @Transactional
    public MessageResponseDTO sendMessage(Long chatId, MessageRequestDTO req) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Xat no trobat"));

        User sender = userRepository.findByGoogleId(req.getSenderGoogleId())
                .orElseThrow(() -> new ResourceNotFoundException("Remitent no trobat"));

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(req.getContent());

        Message savedMessage = messageRepository.save(message);
        return new MessageResponseDTO(savedMessage);
    }

    public List<MessageResponseDTO> getMessagesByChatId(Long chatId) {
        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId).stream()
                .map(MessageResponseDTO::new)
                .toList();
    }

    @Transactional
    public void markMessageAsRead(Long messageId, String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat"));

        messageRepository.markAsRead(messageId, user.getId());
    }

    public List<ChatResponseDTO> getUserChats(String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat"));

        return chatRepository.findChatsByUserId(user.getId()).stream()
                .map(ChatResponseDTO::new)
                .toList();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteOldReadMessages() {
        int deletedCount = messageRepository.deleteMessagesReadOlderThan24Hours();
        if (deletedCount > 0) {
            logger.info("Neteja automàtica: S'han esborrat {} missatges de xat caducats (>24h).", deletedCount);
        }
    }
}