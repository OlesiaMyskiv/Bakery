package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.*;
import com.bakery.Bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserService userService;

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        List<Order.OrderStatus> inactive = Arrays.asList(Order.OrderStatus.DONE, Order.OrderStatus.CANCELLED);

        List<Order> activeOrders = orderRepository.findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(user.getId(), inactive);

        List<Order> historyOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.DONE || o.getOrderStatus() == Order.OrderStatus.CANCELLED)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("historyOrders", historyOrders);
        return "profile";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        orderRepository.findById(id).ifPresent(order -> {
            if (order.getUser() != null && order.getUser().getId().equals(user.getId()) && order.getOrderStatus() == Order.OrderStatus.NEW) {
                order.setOrderStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
            }
        });
        return "redirect:/profile";
    }

    @GetMapping("/api/my-orders")
    @ResponseBody
    public ResponseEntity<?> getMyOrders() {
        User user = userService.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("composition", o.getComposition() != null ? o.getComposition() : "Замовлення №" + o.getId());
            map.put("status", o.getOrderStatus().name());
            map.put("statusLabel", o.getOrderStatus().getLabel());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }
}