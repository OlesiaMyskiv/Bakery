package com.bakery.Bakery.service;

import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Скидання пароля без email.
 * Генерує тимчасовий пароль → зберігає хешований → повертає відкритий для показу на екрані.
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Генерує тимчасовий пароль для юзера за email.
     * @return тимчасовий пароль (відкритий текст) щоб показати на екрані,
     *         або null якщо email не знайдено
     */
    @Transactional
    public String generateTempPassword(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return null;

        String tempPassword = generateRandom();
        user.setPassword(passwordEncoder.encode(tempPassword));
        // Очищаємо токени якщо були
        user.setResetToken(null);
        user.setResetTokenExp(null);
        userRepository.save(user);

        return tempPassword;
    }

    /**
     * Генерує 8-символьний пароль без схожих символів (0/O, 1/l/I).
     */
    private String generateRandom() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        var random = new java.security.SecureRandom();
        var sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}