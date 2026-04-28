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
    @Autowired private ChatMessageRepository chatMessageRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // ============================================================
    // ПРОФІЛЬ — завантажуємо замовлення з бази
    // ============================================================
    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        // Активні замовлення (не DONE і не CANCELLED)
        List<Order.OrderStatus> inactive = Arrays.asList(
                Order.OrderStatus.DONE, Order.OrderStatus.CANCELLED);
        List<Order> activeOrders = orderRepository
                .findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(user.getId(), inactive);

        // Історія (DONE і CANCELLED)
        List<Order> historyOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE
                        || o.getOrderStatus() == Order.OrderStatus.CANCELLED)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("historyOrders", historyOrders);
        return "profile";
    }

    // ============================================================
    // СКАСУВАТИ ЗАМОВЛЕННЯ (клієнт може тільки NEW)
    // ============================================================
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        orderRepository.findById(id).ifPresent(order -> {
            // Клієнт може скасувати тільки якщо замовлення нове
            if (order.getUser() != null
                    && order.getUser().getId().equals(user.getId())
                    && order.getOrderStatus() == Order.OrderStatus.NEW) {
                order.setOrderStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
            }
        });
        return "redirect:/profile";
    }

    // ============================================================
    // ЧАТ — отримати повідомлення для замовлення (JSON)
    // ============================================================
    @GetMapping("/chat/{orderId}/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(@PathVariable Long orderId) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        // Перевіряємо що замовлення належить цьому юзеру (або він адмін)
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();

        boolean isOwner = order.getUser() != null
                && order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) return ResponseEntity.status(403).build();

        // Позначаємо як прочитані якщо клієнт відкрив
        if (isOwner) {
            List<ChatMessage> messages = chatMessageRepository
                    .findByOrderIdOrderBySentAtAsc(orderId);
            messages.forEach(m -> {
                if (!m.isReadByClient()) {
                    m.setReadByClient(true);
                    chatMessageRepository.save(m);
                }
            });
        }

        List<ChatMessage> messages = chatMessageRepository
                .findByOrderIdOrderBySentAtAsc(orderId);

        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("text", m.getText());
            map.put("senderName", m.getSender().getUsername());
            map.put("isAdmin", m.getSender().getRole() == Role.SUPER_ADMIN);
            map.put("sentAt", m.getSentAt().toString());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ============================================================
    // ЧАТ — надіслати повідомлення (JSON)
    // ============================================================
    @PostMapping("/chat/{orderId}/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@PathVariable Long orderId,
                                         @RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();

        boolean isOwner = order.getUser() != null
                && order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) return ResponseEntity.status(403).build();

        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessage msg = new ChatMessage();
        msg.setOrder(order);
        msg.setSender(user);
        msg.setText(text.trim());
        msg.setReadByClient(isOwner); // якщо сам клієнт пише — одразу прочитано
        chatMessageRepository.save(msg);

        Map<String, Object> response = new HashMap<>();
        response.put("id", msg.getId());
        response.put("text", msg.getText());
        response.put("senderName", user.getUsername());
        response.put("isAdmin", isAdmin);
        response.put("sentAt", msg.getSentAt().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/my-orders")
    @ResponseBody
    public ResponseEntity<?> getMyOrders() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", o.getId());
            map.put("composition",
                    o.getComposition() != null ? o.getComposition() : "Замовлення №" + o.getId());
            map.put("status", o.getOrderStatus().name());
            map.put("statusLabel", o.getOrderStatus().getLabel());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
