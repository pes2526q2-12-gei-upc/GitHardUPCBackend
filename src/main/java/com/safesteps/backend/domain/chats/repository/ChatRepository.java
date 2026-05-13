package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT cp.chat FROM ChatParticipant cp WHERE cp.user.id = :userId")
    List<Chat> findChatsByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM chats WHERE id NOT IN (SELECT DISTINCT chat_id FROM chat_participants)", nativeQuery = true)
    int deleteChatsWithNoParticipants();
}