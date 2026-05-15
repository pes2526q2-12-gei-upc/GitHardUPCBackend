package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdOrderByCreatedAtAsc(Long chatId);

    @Modifying
    @Query(value = "INSERT INTO message_read_status (message_id, user_id) VALUES (:messageId, :userId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void markAsRead(Long messageId, Long userId);

    @Modifying
    @Query(value = """
        DELETE FROM messages 
        WHERE id IN (
            SELECT m.id
            FROM messages m
            JOIN chat_participants cp ON m.chat_id = cp.chat_id
            LEFT JOIN message_read_status mrs ON m.id = mrs.message_id AND cp.user_id = mrs.user_id
            WHERE cp.user_id != m.sender_id
            GROUP BY m.id
            HAVING COUNT(cp.user_id) = COUNT(mrs.user_id) 
               AND MAX(mrs.read_at) <= NOW() - INTERVAL '24 HOURS'
        )
    """, nativeQuery = true)
    int deleteMessagesReadOlderThan24Hours();
}