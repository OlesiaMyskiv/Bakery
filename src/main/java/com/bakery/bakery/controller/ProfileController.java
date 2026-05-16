package com.bakery.bakery.controller;

import com.bakery.bakery.model.Order;
import com.bakery.bakery.model.Review;
import com.bakery.bakery.model.User;
import com.bakery.bakery.service.OrderService;
import com.bakery.bakery.service.ReviewService;
import com.bakery.bakery.service.UserService;
import com.bakery.bakery.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import com.bakery.bakery.service.BonusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ProfileController {

    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Autowired(required = false)
    private BonusService bonusService;

    public ProfileController(OrderService orderService, UserService userService, ReviewService reviewService) {
        this.orderService = orderService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("activeOrders",  orderService.findActiveByUser(user.getId()));
        model.addAttribute("historyOrders", orderService.findHistoryByUser(user.getId()));

        // Замовлення DONE без відгуку — для секції "Очікують вашої оцінки"
        List<Order> doneOrders = orderService.findHistoryByUser(user.getId()).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE)
                .filter(o -> !reviewService.hasReviewForOrder(user.getId(), o.getId()))
                .toList();
        model.addAttribute("pendingReviewOrders", doneOrders);
        model.addAttribute("myReviews", reviewService.findByUser(user.getId()));

        // Бонуси
        if (bonusService != null) {
            model.addAttribute("bonusDashboard", bonusService.getDashboard(user.getId()));
        }

        return "profile";
    }

    @GetMapping("/api/bonus-balance")
    @ResponseBody
    public ResponseEntity<?> getBonusBalance() {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        if (bonusService == null) return ResponseEntity.ok(java.util.Map.of("balance", 0));
        int balance = bonusService.getBalance(user.getId());
        return ResponseEntity.ok(java.util.Map.of("balance", balance));
    }

    /** REST endpoint для JS-виклику (чат: «до якого замовлення прив'язати відгук?»). */
    @GetMapping("/api/my-orders")
    @ResponseBody
    public ResponseEntity<?> getMyOrders() {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<Map<String, Object>> result = orderService
                .findActiveByUser(user.getId())
                .stream()
                .map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", o.getId());
                    map.put("composition", o.getComposition() != null
                            ? o.getComposition() : "Замовлення #" + o.getId());
                    map.put("status", o.getOrderStatus().name());
                    map.put("statusLabel", o.getOrderStatus().getLabel());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    /** Дані замовлення для форми відгуку */
    @GetMapping("/api/orders/{id}/review-info")
    @ResponseBody
    public ResponseEntity<?> reviewInfo(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Order order = orderService.findByIdOrThrow(id);
        if (!order.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        // already reviewed?
        if (reviewService.hasReviewForOrder(user.getId(), id))
            return ResponseEntity.status(409).body(Map.of("error", "Ви вже залишили відгук на це замовлення"));

        Map<String, Object> info = new HashMap<>();
        info.put("orderId", order.getId());
        info.put("composition", order.getComposition() != null ? order.getComposition() : "Замовлення #" + order.getId());
        info.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
        // image
        String img = order.getAiImageUrl() != null ? order.getAiImageUrl()
                : order.getConstructorImg() != null ? order.getConstructorImg()
                : order.getFirstImagePath() != null ? order.getFirstImagePath() : null;
        info.put("image", img);
        return ResponseEntity.ok(info);
    }

    /** Надіслати відгук */
    @PostMapping("/api/reviews")
    @ResponseBody
    public ResponseEntity<?> submitReview(
            @RequestParam Long orderId,
            @RequestParam int rating,
            @RequestParam String text,
            @RequestParam(required = false) MultipartFile photo) throws IOException {

        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Order order = orderService.findByIdOrThrow(orderId);
        if (!order.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        if (reviewService.hasReviewForOrder(user.getId(), orderId))
            return ResponseEntity.status(409).body(Map.of("error", "Відгук вже існує"));

        Review review = new Review();
        review.setUser(user);
        review.setOrder(order);
        review.setRating(rating);
        review.setText(text);

        // Зберігаємо фото відгуку через FileStorageService
        if (photo != null && !photo.isEmpty() && fileStorageService != null) {
            try {
                String savedPath = fileStorageService.saveFile(photo, "uploads/review-photos");
                review.setPhotoPath(savedPath);
            } catch (Exception e) {
                // Фото не критичне — продовжуємо без нього
            }
        }

        reviewService.save(review);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** Видалити власний відгук */
    @DeleteMapping("/api/reviews/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        var reviews = reviewService.findByUser(user.getId());
        boolean owns = reviews.stream().anyMatch(r -> r.getId().equals(id));
        if (!owns) return ResponseEntity.status(403).build();

        reviewService.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Сторінка донату бонусів ───────────────────────────────────────────────
    @GetMapping("/bonus-donate")
    public ResponseEntity<?> bonusDonatePage() {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(302).header("Location", "/login").build();
        // Redirect до profile з якоюсь вкладкою — повертаємо JSON для AJAX
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @PostMapping("/api/bonus/donate")
    @ResponseBody
    public ResponseEntity<?> donateBonuses(
            @RequestBody java.util.Map<String, Object> body) {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));

        int amount;
        String fund;
        try {
            amount = Integer.parseInt(String.valueOf(body.get("amount")));
            fund   = String.valueOf(body.getOrDefault("fund", "Невідомий фонд"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid params"));
        }

        if (amount <= 0) return ResponseEntity.badRequest().body(java.util.Map.of("error", "Amount must be > 0"));

        int donated = bonusService.donateToFund(user, amount, fund);
        if (donated == 0) return ResponseEntity.badRequest().body(java.util.Map.of("error", "Недостатньо бонусів"));

        int newBalance = bonusService.getBalance(user.getId());
        return ResponseEntity.ok(java.util.Map.of(
                "donated",    donated,
                "fund",       fund,
                "newBalance", newBalance
        ));
    }

}