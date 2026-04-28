package com.bakery.Bakery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "composition", columnDefinition = "TEXT")
    private String composition;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "price")
    private Integer price;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "address")
    private String address;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "ai_image_url")
    private String aiImageUrl;

    @Column(name = "constructor_img")
    private String constructorImg;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.ON_DELIVERY;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus = OrderStatus.NEW;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    public enum PaymentStatus {
        ON_DELIVERY("При отриманні"),
        PAID("Оплачено");

        private final String label;
        PaymentStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum OrderStatus {
        NEW("Нові"),
        IN_PROGRESS("Готуються"),
        READY("Готові до видачі"),
        DONE("Виконані"),
        CANCELLED("Скасовані");

        private final String label;
        OrderStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getAiImageUrl() { return aiImageUrl; }
    public void setAiImageUrl(String aiImageUrl) { this.aiImageUrl = aiImageUrl; }

    public String getConstructorImg() { return constructorImg; }
    public void setConstructorImg(String constructorImg) { this.constructorImg = constructorImg; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}