package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.User;
import com.bakery.Bakery.service.OrderService;
import com.bakery.Bakery.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    private final OrderService orderService;
    private final UserService userService;

    public ProfileController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("activeOrders",  orderService.findActiveByUser(user.getId()));
        model.addAttribute("historyOrders", orderService.findHistoryByUser(user.getId()));
        return "profile";
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
}