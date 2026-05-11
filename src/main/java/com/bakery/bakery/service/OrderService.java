package com.bakery.bakery.service;

import com.bakery.bakery.dto.CreateOrderDTO;
import com.bakery.bakery.model.Order;
import com.bakery.bakery.service.BonusService;
import com.bakery.bakery.model.User;
import com.bakery.bakery.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true) // всі методи read-only за замовчуванням; мутуючі — перевизначають
public class OrderService {

    private final OrderRepository orderRepository;
    @Autowired(required = false) private BonusService bonusService;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── Читання ───────────────────────────────────────────────────────────────

    public List<Order> findAllByStatus(Order.OrderStatus status) {
        return orderRepository.findByOrderStatusOrderByCreatedAtDesc(status);
    }

    public List<Order> findAll() {
        return orderRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public Order findByIdOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Замовлення #" + id + " не знайдено"));
    }

    public List<Order> findActiveByUser(Long userId) {
        return orderRepository.findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(
                userId,
                List.of(Order.OrderStatus.DONE, Order.OrderStatus.CANCELLED)
        );
    }

    public List<Order> findHistoryByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE
                        || o.getOrderStatus() == Order.OrderStatus.CANCELLED)
                .toList();
    }

    /** Всі замовлення юзера — для адмін-панелі */
    public List<Order> findAllByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countByStatus(Order.OrderStatus status) {
        return orderRepository.findByOrderStatusOrderByCreatedAtDesc(status).size();
    }

    // ── Запис ─────────────────────────────────────────────────────────────────

    @Transactional
    public Order createOrder(CreateOrderDTO dto, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setComposition(dto.getComposition());
        order.setAddress(dto.getAddress());
        order.setWeightKg(dto.getWeightKg());
        order.setComment(dto.getComment());
        order.setAiImageUrl(dto.getAiImageUrl());
        order.setConstructorImg(dto.getConstructorImg());
        order.setOrderStatus(Order.OrderStatus.NEW);
        order.setPaymentStatus(Order.PaymentStatus.ON_DELIVERY);
        order.setCreatedAt(LocalDateTime.now());

        if (dto.getDeadline() != null && !dto.getDeadline().isBlank()) {
            order.setDeadline(LocalDateTime.parse(dto.getDeadline(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        }

        return orderRepository.save(order);
    }

    @Transactional
    public void updateStatus(Long id, Order.OrderStatus newStatus) {
        Order order = findByIdOrThrow(id);
        Order.OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        // Нараховуємо бонуси коли замовлення стає ВИКОНАНИМ
        if (newStatus == Order.OrderStatus.DONE
                && oldStatus != Order.OrderStatus.DONE
                && bonusService != null
                && order.getUser() != null) {
            bonusService.earnForOrder(order.getUser(), order);
        }
    }

    @Transactional
    public void cancelByUser(Long orderId, Long userId) {
        Order order = findByIdOrThrow(orderId);

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new SecurityException("Немає прав на це замовлення");
        }
        if (order.getOrderStatus() != Order.OrderStatus.NEW) {
            throw new IllegalStateException("Скасувати можна лише нові замовлення");
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}