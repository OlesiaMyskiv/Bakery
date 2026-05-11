package com.bakery.bakery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сесія чату — може бути від зареєстрованого юзера або від гостя.
 * Кожен унікальний "розмовник" має одну сесію.
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Зареєстрований юзер (null якщо гість)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Унікальний токен сесії для гостя (зберігається в localStorage)
    @Column(name = "guest_token", unique = true)
    private String guestToken;

    // Ім'я гостя (якщо вказав)
    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt = LocalDateTime.now();

    // Чи є непрочитані повідомлення від адміна (для клієнта/гостя)
    @Column(name = "unread_for_client")
    private boolean unreadForClient = false;

    // Чи є непрочитані для адміна
    @Column(name = "unread_for_admin")
    private boolean unreadForAdmin = false;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGuestToken() { return guestToken; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public boolean isUnreadForClient() { return unreadForClient; }
    public void setUnreadForClient(boolean unreadForClient) { this.unreadForClient = unreadForClient; }

    public boolean isUnreadForAdmin() { return unreadForAdmin; }
    public void setUnreadForAdmin(boolean unreadForAdmin) { this.unreadForAdmin = unreadForAdmin; }

    // Зручний метод — отримати відображуване ім'я
    public String getDisplayName() {
        if (user != null) return user.getUsername();
        if (guestName != null && !guestName.isBlank()) return guestName;
        return "Гість";
    }
}