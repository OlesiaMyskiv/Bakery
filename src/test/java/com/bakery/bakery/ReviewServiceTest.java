package com.bakery.bakery.service;

import com.bakery.bakery.model.Review;
import com.bakery.bakery.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("reply: зберігає відповідь адміна")
    void reply_savesAdminReply() {
        Review review = new Review();
        review.setId(1L);
        review.setText("Чудовий торт!");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        reviewService.reply(1L, "Дякуємо!");

        assertThat(review.getAdminReply()).isEqualTo("Дякуємо!");
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("toggleVisibility: приховує видимий відгук")
    void toggleVisibility_hidesVisibleReview() {
        Review review = new Review();
        review.setId(2L);
        review.setHidden(false);

        when(reviewRepository.findById(2L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        reviewService.toggleVisibility(2L);

        assertThat(review.isHidden()).isTrue();
    }

    @Test
    @DisplayName("toggleVisibility: показує прихований відгук")
    void toggleVisibility_showsHiddenReview() {
        Review review = new Review();
        review.setId(3L);
        review.setHidden(true);

        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        reviewService.toggleVisibility(3L);

        assertThat(review.isHidden()).isFalse();
    }

    @Test
    @DisplayName("reply: кидає IllegalArgumentException якщо відгук не знайдено")
    void reply_throwsIfNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.reply(999L, "text"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
