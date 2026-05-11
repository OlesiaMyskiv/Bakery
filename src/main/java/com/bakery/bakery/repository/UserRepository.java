package com.bakery.bakery.repository;

import com.bakery.bakery.model.User;
import com.bakery.bakery.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    /** Пошук юзера за reset-токеном (для сторінки введення нового пароля). */
    Optional<User> findByResetToken(String resetToken);

    List<User> findByVerificationStatusOrderByIdDesc(VerificationStatus status);

    long countByVerificationStatus(VerificationStatus status);
}