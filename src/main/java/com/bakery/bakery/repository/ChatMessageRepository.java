package com.bakery.bakery.repository;

import com.bakery.bakery.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);
    long countBySessionIdAndFromAdminFalse(Long sessionId); // непрочитані від клієнта
}
