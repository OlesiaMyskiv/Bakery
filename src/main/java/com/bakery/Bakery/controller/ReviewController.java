package com.bakery.bakery.controller;

import com.bakery.bakery.model.*;
import com.bakery.bakery.repository.*;
import com.bakery.bakery.service.FileStorageService;
import com.bakery.bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReviewController {

    @Autowired private ReviewRepository   reviewRepository;
    @Autowired private OrderRepository    orderRepository;
    @Autowired private UserService        userService;
    @Autowired private FileStorageService fileStorageService;

    @PostMapping("/reviews/submit")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> submitReview(
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam String text,
            @RequestParam(required = false) MultipartFile photo) {

        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (!order.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();
        if (order.getOrderStatus() != Order.OrderStatus.DONE)
            return ResponseEntity.badRequest().body("Замовлення ще не виконане");

        boolean alreadyReviewed = reviewRepository.findAll().stream()
                .anyMatch(r -> r.getOrder() != null
                        && r.getOrder().getId().equals(orderId)
                        && r.getUser().getId().equals(user.getId()));
        if (alreadyReviewed)
            return ResponseEntity.badRequest().body("Ви вже залишили відгук");

        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            try { photoPath = fileStorageService.saveFile(photo, "uploads/review-photos"); }
            catch (Exception ignored) {}
        }

        Review review = new Review();
        review.setUser(user);
        review.setOrder(order);
        review.setText(text.trim());
        review.setRating(Math.max(1, Math.min(5, rating)));
        review.setPhotoPath(photoPath);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        return ResponseEntity.ok(Map.of("ok", true, "id", review.getId()));
    }

    @GetMapping("/api/my-reviews")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyReviews() {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<Map<String, Object>> myReviews = reviewRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",         r.getId());
                    m.put("rating",     r.getRating());
                    m.put("text",       r.getText());
                    m.put("photoPath",  r.getPhotoPath());
                    m.put("adminReply", r.getAdminReply());
                    m.put("createdAt",  r.getCreatedAt() != null
                            ? r.getCreatedAt().toLocalDate().toString() : "");
                    m.put("hidden",     r.isHidden());

                    String comp = r.getOrder() != null
                            ? r.getOrder().getComposition() : "";
                    String name = comp != null && comp.contains(" — ")
                            ? comp.split("[|;]")[0].trim().split(" — ")[0].trim()
                            : (comp != null ? comp : "Замовлення");
                    m.put("productName", name);
                    m.put("orderId", r.getOrder() != null ? r.getOrder().getId() : null);

                    String img = null;
                    if (r.getOrder() != null) {
                        if (r.getOrder().getFirstImagePath() != null)
                            img = r.getOrder().getFirstImagePath();
                        else if (r.getOrder().getAiImageUrl() != null)
                            img = r.getOrder().getAiImageUrl();
                    }
                    m.put("productImage", img);
                    return m;
                })
                .collect(Collectors.toList());

        Set<Long> reviewedOrderIds = reviewRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(user.getId()) && r.getOrder() != null)
                .map(r -> r.getOrder().getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> pendingOrders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE
                        && !reviewedOrderIds.contains(o.getId()))
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("orderId", o.getId());
                    String comp = o.getComposition() != null ? o.getComposition() : "";
                    String name = comp.contains(" — ")
                            ? comp.split("[|;]")[0].trim().split(" — ")[0].trim()
                            : comp;
                    m.put("productName", name);
                    String img = o.getFirstImagePath() != null ? o.getFirstImagePath()
                            : (o.getAiImageUrl() != null ? o.getAiImageUrl() : null);
                    m.put("productImage", img);
                    m.put("orderDate", o.getCreatedAt() != null
                            ? o.getCreatedAt().toLocalDate().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "reviews", myReviews,
                "pendingOrders", pendingOrders
        ));
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteMyReview(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        if (!review.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();
        reviewRepository.delete(review);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/admin/reviews/{id}/toggle-hidden")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> toggleHidden(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null || user.getRole() != Role.SUPER_ADMIN)
            return ResponseEntity.status(403).build();
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        review.setHidden(!review.isHidden());
        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of("hidden", review.isHidden()));
    }
}