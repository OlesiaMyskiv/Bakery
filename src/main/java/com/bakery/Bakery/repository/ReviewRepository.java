// ===================== ReviewRepository.java =====================
package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHiddenFalseOrderByCreatedAtDesc();
    List<Review> findByAdminReplyIsNullAndHiddenFalseOrderByCreatedAtDesc();
    List<Review> findAllByOrderByCreatedAtDesc();
    long countByAdminReplyIsNullAndHiddenFalse();
}
