package com.bakery.Bakery.dto;

import jakarta.validation.constraints.*;

/**
 * DTO для оформлення нового замовлення клієнтом.
 */
public class CreateOrderDTO {

    @NotBlank(message = "Склад замовлення обов'язковий")
    @Size(max = 2000, message = "Опис занадто довгий")
    private String composition;

    @NotBlank(message = "Адреса обов'язкова")
    @Size(max = 300)
    private String address;

    /** Формат: yyyy-MM-ddTHH:mm (HTML datetime-local) */
    @NotBlank(message = "Дедлайн обов'язковий")
    private String deadline;

    @Min(value = 1, message = "Вага має бути більше 0")
    private Double weightKg;

    @Size(max = 1000)
    private String comment;

    /** URL зображення від AI (може бути null) */
    private String aiImageUrl;

    /** Шлях до зображення з конструктора (може бути null) */
    private String constructorImg;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getAiImageUrl() { return aiImageUrl; }
    public void setAiImageUrl(String aiImageUrl) { this.aiImageUrl = aiImageUrl; }

    public String getConstructorImg() { return constructorImg; }
    public void setConstructorImg(String constructorImg) { this.constructorImg = constructorImg; }
}
