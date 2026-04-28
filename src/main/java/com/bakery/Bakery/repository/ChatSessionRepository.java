package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // Знайти сесію зареєстрованого юзера
    Optional<ChatSession> findByUserId(Long userId);

    // Знайти сесію гостя за токеном
    Optional<ChatSession> findByGuestToken(String token);

    // Всі сесії для адмін-панелі (відсортовані за останнім повідомленням)
    List<ChatSession> findAllByOrderByLastMessageAtDesc();
}
