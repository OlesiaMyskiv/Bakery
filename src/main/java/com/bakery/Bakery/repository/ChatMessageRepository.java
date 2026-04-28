package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByOrderIdOrderBySentAtAsc(Long orderId);
    long countByOrderIdAndReadByClientFalse(Long orderId);
}
