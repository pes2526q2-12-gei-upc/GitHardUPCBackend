package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

    @Transactional
    void deleteByChatIdAndUserId(Long chatId, Long userId);

    long countByChatId(Long chatId);

    long countByChatIdAndRole(Long chatId, String role);

    List<ChatParticipant> findByChatIdOrderByJoinedAtAsc(Long chatId);
}