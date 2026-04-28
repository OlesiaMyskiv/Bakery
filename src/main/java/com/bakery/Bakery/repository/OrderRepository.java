// ===================== OrderRepository.java =====================
package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Orderі;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Orderі, Long> {
    List<Orderі> findByOrderStatusOrderByCreatedAtDesc(Orderі.OrderStatus status);
    List<Orderі> findAllByOrderByCreatedAtDesc();
}
