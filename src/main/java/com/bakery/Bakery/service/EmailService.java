package com.bakery.Bakery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Відповідає лише за відправку email.
 * Формування тексту — тут; рішення "коли відправляти" — у викликаючому сервісі.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Надсилає тимчасовий пароль користувачу після reset.
     */
    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Відновлення пароля — CakeHouse");
        msg.setText("""
                Вітаємо!
                
                Ваш тимчасовий пароль: %s
                
                Після входу рекомендуємо змінити його у налаштуваннях профілю.
                
                З повагою, команда CakeHouse 🍰
                """.formatted(tempPassword));
        mailSender.send(msg);
    }

    /**
     * Надсилає підтвердження замовлення.
     */
    public void sendOrderConfirmation(String toEmail, Long orderId) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Замовлення #" + orderId + " прийнято — CakeHouse");
        msg.setText("""
                Дякуємо за замовлення!
                
                Ваше замовлення #%d успішно прийнято і вже опрацьовується.
                Відстежувати статус можна у своєму профілі.
                
                З повагою, команда CakeHouse 🍰
                """.formatted(orderId));
        mailSender.send(msg);
    }

    /**
     * Загальний метод для довільних повідомлень.
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
