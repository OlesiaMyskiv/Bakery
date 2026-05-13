package com.bakery.bakery.repository;

import com.bakery.bakery.model.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findByHiddenFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findByAdminReplyIsNullAndHiddenFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findAllByOrderByCreatedAtDesc();

    long countByAdminReplyIsNullAndHiddenFalse();

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findByProductIdAndHiddenFalse(Long productId);

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndOrderId(Long userId, Long orderId);

    @EntityGraph(attributePaths = {"user", "order"})
    List<Review> findTop3ByHiddenFalseOrderByCreatedAtDesc();
}