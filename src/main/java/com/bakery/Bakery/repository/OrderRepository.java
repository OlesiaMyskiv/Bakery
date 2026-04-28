package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Для адміна
    List<Order> findByOrderStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();

    // Для клієнта — всі замовлення
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Для клієнта — активні (не виконані і не скасовані)
    List<Order> findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(
            Long userId, List<Order.OrderStatus> excludedStatuses);
}
