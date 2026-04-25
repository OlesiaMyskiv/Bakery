package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // Додай цей імпорт

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // Тепер він повертає Optional
}