package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT cp.chat FROM ChatParticipant cp WHERE cp.user.id = :userId")
    List<Chat> findChatsByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM chats WHERE id NOT IN (SELECT DISTINCT chat_id FROM chat_participants)", nativeQuery = true)
    int deleteChatsWithNoParticipants();

    @Query("SELECT cp1.chat FROM ChatParticipant cp1 JOIN ChatParticipant cp2 ON cp1.chat.id = cp2.chat.id " +
            "WHERE cp1.chat.type = 'PRIVATE' AND cp1.user.googleId = :user1 AND cp2.user.googleId = :user2")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1") String user1, @Param("user2") String user2);
}