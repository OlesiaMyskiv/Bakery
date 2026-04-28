package com.bakery.Bakery.dto;

import jakarta.validation.constraints.*;

/**
 * DTO для редагування профілю користувача.
 */
public class EditProfileDTO {

    @NotBlank(message = "Ім'я не може бути порожнім")
    @Size(min = 2, max = 60)
    private String username;

    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Невірний формат email")
    private String email;

    @NotBlank(message = "Телефон обов'язковий")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Невірний формат телефону")
    private String phone;

    /** Якщо порожній — пароль не змінюється */
    @Size(min = 6, max = 100, message = "Пароль має бути від 6 до 100 символів")
    private String password;

    /** Формат: yyyy-MM-dd (як надсилає HTML input type="date") */
    private String birthDate;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}
