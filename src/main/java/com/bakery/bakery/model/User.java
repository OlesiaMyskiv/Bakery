package com.bakery.bakery.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String phone;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean consent;

    @Column(name = "document_path")
    private String documentPath;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    @Column(name = "profile_picture_path")
    private String profilePicturePath;

    // ── НОВІ ПОЛЯ ДЛЯ СКИДАННЯ ПАРОЛЯ ────────────────────────────────────────

    /** Унікальний токен що надсилається на пошту. Null якщо reset не запитувався. */
    @Column(name = "reset_token", unique = true)
    private String resetToken;

    /** Коли токен закінчується (now + 5 хвилин). */
    @Column(name = "reset_token_exp")
    private LocalDateTime resetTokenExp;

    // ── Конструктор ───────────────────────────────────────────────────────────

    public User() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus v) { this.verificationStatus = v; }

    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String p) { this.profilePicturePath = p; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExp() { return resetTokenExp; }
    public void setResetTokenExp(LocalDateTime resetTokenExp) { this.resetTokenExp = resetTokenExp; }

    /** Перевірка: токен ще дійсний? */
    public boolean isResetTokenValid() {
        return resetToken != null
                && resetTokenExp != null
                && LocalDateTime.now().isBefore(resetTokenExp);
    }
}