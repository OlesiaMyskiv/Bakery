package com.bakery.bakery.service;

import com.bakery.bakery.model.Review;
import com.bakery.bakery.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // ── Читання ───────────────────────────────────────────────────────────────

    public List<Review> findVisible() {
        return reviewRepository.findByHiddenFalseOrderByCreatedAtDesc();
    }

    public List<Review> findUnanswered() {
        return reviewRepository.findByAdminReplyIsNullAndHiddenFalseOrderByCreatedAtDesc();
    }

    public List<Review> findAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Review> findHidden() {
        return findAll().stream().filter(Review::isHidden).toList();
    }

    public List<Review> findByProduct(Long productId) {
        return reviewRepository.findByProductIdAndHiddenFalse(productId);
    }

    public List<Review> findByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public boolean hasReviewForOrder(Long userId, Long orderId) {
        return reviewRepository.existsByUserIdAndOrderId(userId, orderId);
    }

    public long countUnanswered() {
        return reviewRepository.countByAdminReplyIsNullAndHiddenFalse();
    }

    // ── Запис ─────────────────────────────────────────────────────────────────

    @Transactional
    public void reply(Long reviewId, String replyText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Відгук #" + reviewId + " не знайдено"));
        review.setAdminReply(replyText);
        reviewRepository.save(review);
    }

    @Transactional
    public void toggleVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Відгук #" + reviewId + " не знайдено"));
        review.setHidden(!review.isHidden());
        reviewRepository.save(review);
    }

    @Transactional
    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteById(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}