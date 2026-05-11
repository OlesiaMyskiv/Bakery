package com.bakery.bakery.dto;

import com.bakery.bakery.model.Role;
import jakarta.validation.constraints.*;

/**
 * DTO для реєстрації нового користувача.
 * Валідація відбувається у контролері через @Valid — жодна бізнес-логіка тут не живе.
 */
public class RegisterDTO {

    @NotBlank(message = "Ім'я не може бути порожнім")
    @Size(min = 2, max = 60, message = "Ім'я має бути від 2 до 60 символів")
    private String username;

    @NotBlank(message = "Телефон обов'язковий")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Невірний формат телефону")
    private String phone;

    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Невірний формат email")
    private String email;

    @NotBlank(message = "Пароль обов'язковий")
    @Size(min = 6, max = 100, message = "Пароль має бути від 6 до 100 символів")
    private String password;

    @NotBlank(message = "Підтвердження пароля обов'язкове")
    private String confirmPassword;

    @NotNull(message = "Роль обов'язкова")
    private Role role;

    private boolean consent;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }

    /** Бізнес-правило: паролі збігаються. Викликати після @Valid. */
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
