package com.bakery.Bakery.service;

import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ── КРОК 1: Юзер вводить email ────────────────────────────────────────────

    /**
     * Генерує унікальний токен, зберігає в users, надсилає посилання на пошту.
     * Завжди повертає true (безпека — не розкривати наявність email).
     */
    @Transactional
    public void sendResetLink(String email) {
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            // Генеруємо унікальний токен
            String token = UUID.randomUUID().toString();

            // Зберігаємо в юзера
            user.setResetToken(token);
            user.setResetTokenExp(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES));
            userRepository.save(user);

            // Надсилаємо посилання на пошту
            emailService.sendResetLink(email, token);
        });
    }

    // ── КРОК 2: Перевірка токена ──────────────────────────────────────────────

    /**
     * Перевіряє чи токен існує і не прострочений.
     * @return User якщо токен валідний, null якщо ні.
     */
    public User validateToken(String token) {
        if (token == null || token.isBlank()) return null;

        User user = userRepository.findByResetToken(token).orElse(null);
        if (user == null || !user.isResetTokenValid()) return null;

        return user;
    }

    // ── КРОК 3: Юзер вводить новий пароль ────────────────────────────────────

    /**
     * Зберігає новий пароль, очищає токен.
     * @return false якщо токен вже недійсний (прострочений або підроблений).
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = validateToken(token);
        if (user == null) return false;

        // Хешуємо і зберігаємо новий пароль
        user.setPassword(passwordEncoder.encode(newPassword));

        // Очищаємо токен — він одноразовий
        user.setResetToken(null);
        user.setResetTokenExp(null);

        userRepository.save(user);
        return true;
    }
}