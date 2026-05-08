package com.safesteps.backend.domain.chats.repository;

import com.safesteps.backend.domain.chats.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT cp.chat FROM ChatParticipant cp WHERE cp.user.id = :userId")
    List<Chat> findChatsByUserId(Long userId);
}
