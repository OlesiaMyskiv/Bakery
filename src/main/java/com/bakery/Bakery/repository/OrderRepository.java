package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Для адміна
    @EntityGraph(attributePaths = {"user"})
    List<Order> findByOrderStatusOrderByCreatedAtDesc(Order.OrderStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Order> findAllByOrderByCreatedAtDesc();

    // Для клієнта — всі замовлення
    @EntityGraph(attributePaths = {"user"})
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Для клієнта — активні (не виконані і не скасовані)
    @EntityGraph(attributePaths = {"user"})
    List<Order> findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(
            Long userId, List<Order.OrderStatus> excludedStatuses);
}
