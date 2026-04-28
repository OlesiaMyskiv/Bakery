package com.bakery.Bakery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    // Базовий URL додатку (для посилань в листах)
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Надсилає посилання для скидання пароля.
     * Посилання дійсне 5 хвилин.
     */
    public void sendResetLink(String toEmail, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Відновлення пароля — CakeHouse 🍰");
        msg.setText("""
                Вітаємо!
                
                Ми отримали запит на відновлення пароля для вашого акаунту.
                
                Натисніть на посилання нижче, щоб створити новий пароль:
                %s
                
                ⚠️ Посилання дійсне лише 5 хвилин.
                
                Якщо ви не робили цього запиту — просто проігноруйте цей лист.
                
                З повагою,
                Команда CakeHouse 🍰
                """.formatted(resetLink));

        mailSender.send(msg);
    }

    /**
     * Підтвердження успішної зміни пароля.
     */
    public void sendPasswordChangedConfirmation(String toEmail) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Пароль змінено — CakeHouse 🍰");
        msg.setText("""
                Вітаємо!
                
                Ваш пароль було успішно змінено.
                
                Якщо це були не ви — негайно зверніться до нас.
                
                З повагою,
                Команда CakeHouse 🍰
                """);
        mailSender.send(msg);
    }

    /**
     * Загальний метод.
     */
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}