package com.bakery.Bakery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Orderі {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "composition", columnDefinition = "TEXT")
    private String composition; // склад замовлення

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "price")
    private Integer price; // ціна в гривнях

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

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
