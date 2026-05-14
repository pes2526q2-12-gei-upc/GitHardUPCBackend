package com.safesteps.backend.domain.chats.service;

import com.safesteps.backend.domain.chats.dto.*;
import com.safesteps.backend.domain.chats.model.*;
import com.safesteps.backend.domain.chats.repository.*;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.common.exception.BadRequestException;
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
    private final ChatParticipantRepository chatParticipantRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private static final String ADMIN = "ADMIN";
    private static final String GROUP = "GROUP";
    private static final String XAT_NO_TROBAT = "Xat no trobat";
    private static final String USUARI_NO_TROBAT = "Usuari no trobat";

    public ChatService(ChatRepository chatRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       ChatParticipantRepository chatParticipantRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatParticipantRepository = chatParticipantRepository;
    }

    @Transactional
    public ChatResponseDTO createChat(ChatRequestDTO req) {
        Chat chat = new Chat();
        chat.setType(req.getType());
        chat.setName(req.getName());

        Chat savedChat = chatRepository.save(chat);

        for (int i = 0; i < req.getParticipantGoogleIds().size(); i++) {
            String gId = req.getParticipantGoogleIds().get(i);
            User user = userRepository.findByGoogleId(gId)
                    .orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT + ": " + gId));

            ChatParticipant participant = new ChatParticipant();
            participant.setChat(savedChat);
            participant.setUser(user);

            if (GROUP.equals(savedChat.getType()) && i == 0) {
                participant.setRole(ADMIN);
            } else {
                participant.setRole(null);
            }

            chatParticipantRepository.save(participant);
        }

        return new ChatResponseDTO(savedChat);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteOldReadMessages() {
        int deletedCount = messageRepository.deleteMessagesReadOlderThan24Hours();
        if (deletedCount > 0) {
            logger.info("Neteja automàtica: S'han esborrat {} missatges llegits fa >24h.", deletedCount);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteEmptyChatsCron() {
        int deletedCount = chatRepository.deleteChatsWithNoParticipants();
        if (deletedCount > 0) {
            logger.info("Neteja diària: S'han eliminat {} xats que no tenien integrants.", deletedCount);
        }
    }

    @Transactional
    public void grantAdmin(Long chatId, String adminGoogleId, String targetUserGoogleId) {
        checkIfUserIsAdmin(chatId, adminGoogleId);

        User targetUser = userRepository.findByGoogleId(targetUserGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari destí no trobat"));

        ChatParticipant participant = chatParticipantRepository.findByChatIdAndUserId(chatId, targetUser.getId())
                .orElseThrow(() -> new BadRequestException("L'usuari no és al grup"));

        participant.setRole(ADMIN);
        chatParticipantRepository.save(participant);
        logger.info("L'usuari {} ara també és ADMIN del grup {}", targetUserGoogleId, chatId);
    }

    @Transactional
    public void exitGroup(Long chatId, String googleId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException(XAT_NO_TROBAT));

        if (!GROUP.equals(chat.getType())) {
            throw new BadRequestException("No pots sortir d'un xat privat.");
        }

        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT));

        ChatParticipant leavingParticipant = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new BadRequestException("No ets participant d'aquest xat"));

        boolean wasAdmin = ADMIN.equals(leavingParticipant.getRole());

        chatParticipantRepository.deleteByChatIdAndUserId(chatId, user.getId());
        logger.info("L'usuari {} ha sortit del grup {}", googleId, chatId);

        long remainingCount = chatParticipantRepository.countByChatId(chatId);
        if (remainingCount == 0) {
            chatRepository.delete(chat);
            logger.info("Grup {} eliminat per falta de participants.", chatId);
            return;
        }

        if (wasAdmin) {
            long adminCount = chatParticipantRepository.countByChatIdAndRole(chatId, ADMIN);

            if (adminCount == 0) {
                List<ChatParticipant> others = chatParticipantRepository.findByChatIdOrderByJoinedAtAsc(chatId);
                if (!others.isEmpty()) {
                    ChatParticipant nextAdmin = others.get(0);
                    nextAdmin.setRole(ADMIN);
                    chatParticipantRepository.save(nextAdmin);
                    logger.info("L'usuari {} ha heretat l'ADMIN del grup {} per antiguitat.",
                            nextAdmin.getUser().getUsername(), chatId);
                }
            }
        }
    }

    @Transactional
    public void addUserToGroup(Long chatId, String adminGoogleId, String newUserGoogleId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException(XAT_NO_TROBAT));

        if (!GROUP.equals(chat.getType())) {
            throw new BadRequestException("Acció només disponible per a grups.");
        }

        checkIfUserIsAdmin(chatId, adminGoogleId);

        User newUser = userRepository.findByGoogleId(newUserGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException("L'usuari a afegir no existeix"));

        ChatParticipant participant = new ChatParticipant();
        participant.setChat(chat);
        participant.setUser(newUser);
        participant.setRole(null);
        chatParticipantRepository.save(participant);
    }

    @Transactional
    public void removeUserFromGroup(Long chatId, String adminGoogleId, String targetUserGoogleId) {
        checkIfUserIsAdmin(chatId, adminGoogleId);

        User targetUser = userRepository.findByGoogleId(targetUserGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT));

        chatParticipantRepository.deleteByChatIdAndUserId(chatId, targetUser.getId());
    }

    private void checkIfUserIsAdmin(Long chatId, String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT));

        ChatParticipant requester = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new BadRequestException("No ets part d'aquest xat"));

        if (!ADMIN.equals(requester.getRole())) {
            throw new BadRequestException("Només els administradors poden fer això.");
        }
    }

    @Transactional
    public MessageResponseDTO sendMessage(Long chatId, MessageRequestDTO req) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ResourceNotFoundException(XAT_NO_TROBAT));
        User sender = userRepository.findByGoogleId(req.getSenderGoogleId()).orElseThrow(() -> new ResourceNotFoundException("Remitent no trobat"));
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(req.getContent());
        return new MessageResponseDTO(messageRepository.save(message));
    }

    public List<MessageResponseDTO> getMessagesByChatId(Long chatId) {
        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId).stream().map(MessageResponseDTO::new).toList();
    }

    @Transactional
    public void markMessageAsRead(Long messageId, String googleId) {
        User user = userRepository.findByGoogleId(googleId).orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT));
        messageRepository.markAsRead(messageId, user.getId());
    }

    public List<ChatResponseDTO> getUserChats(String googleId) {
        User user = userRepository.findByGoogleId(googleId).orElseThrow(() -> new ResourceNotFoundException(USUARI_NO_TROBAT));
        return chatRepository.findChatsByUserId(user.getId()).stream().map(ChatResponseDTO::new).toList();
    }

    @Transactional
    public void revokeAdmin(Long chatId, String adminGoogleId, String targetUserGoogleId) {
        checkIfUserIsAdmin(chatId, adminGoogleId);

        User targetUser = userRepository.findByGoogleId(targetUserGoogleId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari destí no trobat"));

        ChatParticipant participant = chatParticipantRepository.findByChatIdAndUserId(chatId, targetUser.getId())
                .orElseThrow(() -> new BadRequestException("L'usuari no és al grup"));

        if (adminGoogleId.equals(targetUserGoogleId)) {
            long adminCount = chatParticipantRepository.countByChatIdAndRole(chatId, ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("No et pots treure el rang d'administrador si ets l'únic que queda.");
            }
        }
        participant.setRole(null);
        chatParticipantRepository.save(participant);
        logger.info("L'usuari {} ja no és ADMIN del grup {}", targetUserGoogleId, chatId);
    }
}