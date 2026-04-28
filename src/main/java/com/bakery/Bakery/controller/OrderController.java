package com.bakery.Bakery.controller;

import com.bakery.Bakery.dto.CreateOrderDTO;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.service.EmailService;
import com.bakery.Bakery.service.OrderService;
import com.bakery.Bakery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final EmailService emailService;

    public OrderController(OrderService orderService,
                           UserService userService,
                           EmailService emailService) {
        this.orderService = orderService;
        this.userService = userService;
        this.emailService = emailService;
    }

    // GET /orders/new — форма нового замовлення
    @GetMapping("/new")
    public String newOrderForm(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        model.addAttribute("orderDto", new CreateOrderDTO());
        return "order-form";
    }

    // POST /orders/new — зберегти замовлення
    @PostMapping("/new")
    public String submitOrder(@Valid @ModelAttribute("orderDto") CreateOrderDTO dto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            return "order-form";
        }

        var saved = orderService.createOrder(dto, user);

        // Підтвердження на пошту — не обривати flow якщо mail впав
        try {
            if (user.getEmail() != null) {
                emailService.sendOrderConfirmation(user.getEmail(), saved.getId());
            }
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("successMsg",
                "Замовлення #" + saved.getId() + " успішно оформлено!");
        return "redirect:/profile";
    }

    // POST /orders/{id}/cancel — скасувати замовлення клієнтом
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        orderService.cancelByUser(id, user.getId());
        redirectAttributes.addFlashAttribute("successMsg", "Замовлення скасовано.");
        return "redirect:/profile";
    }
}