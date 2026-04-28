package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    // ChatMessageRepository тут більше НЕ потрібен — чат переїхав у ChatController

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // ── ПРОФІЛЬ ──────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        List<Order.OrderStatus> inactive = Arrays.asList(
                Order.OrderStatus.DONE, Order.OrderStatus.CANCELLED);

        List<Order> activeOrders = orderRepository
                .findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(user.getId(), inactive);

        List<Order> historyOrders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE
                        || o.getOrderStatus() == Order.OrderStatus.CANCELLED)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("historyOrders", historyOrders);
        return "profile";
    }

    // ── СКАСУВАТИ ЗАМОВЛЕННЯ ─────────────────────────────────────────────
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        orderRepository.findById(id).ifPresent(order -> {
            if (order.getUser() != null
                    && order.getUser().getId().equals(user.getId())
                    && order.getOrderStatus() == Order.OrderStatus.NEW) {
                order.setOrderStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
            }
        });
        return "redirect:/profile";
    }

    // ── API: МОЇ ЗАМОВЛЕННЯ (для чату — вибір замовлення) ───────────────
    @GetMapping("/api/my-orders")
    @ResponseBody
    public ResponseEntity<?> getMyOrders() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("composition",
                    o.getComposition() != null ? o.getComposition() : "Замовлення №" + o.getId());
            map.put("status", o.getOrderStatus().name());
            map.put("statusLabel", o.getOrderStatus().getLabel());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // УВАГА: методи /chat/{orderId}/messages і /chat/{orderId}/send
    // видалені звідси — вони тепер у ChatController.java
    // (/api/chat/{sessionId}/messages і /api/chat/{sessionId}/send)
}
