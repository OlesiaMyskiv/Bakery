package com.bakery.bakery.repository;

import com.bakery.bakery.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

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

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.id = :id")
    Optional<Order> findByIdWithUser(@Param("id") Long id);

    List<Order> findTop100ByOrderByCreatedAtDesc();
}
