package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Order;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.OrderRepository;
import com.bakery.Bakery.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired private UserService userService;
    @Autowired private OrderRepository orderRepository;

    /**
     * GET /cart — сторінка кошика
     */
    @GetMapping
    public String cartPage(Model model) {
        User user = userService.getCurrentUser();

        // Статус верифікації для JS (щоб показувати пільгову ціну)
        boolean isVerified = user != null &&
                user.getVerificationStatus() != null &&
                user.getVerificationStatus().name().equals("APPROVED") &&
                user.getRole() != null &&
                !user.getRole().name().equals("SUPER_ADMIN");

        model.addAttribute("isVerified", isVerified);
        return "cart";
    }

    /**
     * POST /cart/checkout — оформлення замовлення з кошика.
     *
     * Параметри (form):
     *   cartJson        — JSON масив елементів кошика
     *   deliveryDate    — yyyy-MM-dd
     *   deliveryTime    — "10:00-12:00"
     *   deliveryType    — "PICKUP" | "DELIVERY"
     *   address         — адреса (якщо DELIVERY)
     *   paymentMethod   — "ON_DELIVERY" | "ONLINE"
     *   paymentStatus   — "ON_DELIVERY" | "PAID"
     *   comment         — коментар
     *   militarySweets  — "true" | "false"
     */
    @PostMapping("/checkout")
    public String checkout(
            @RequestParam(required = false) String cartJson,
            @RequestParam(required = false) String deliveryDate,
            @RequestParam(required = false) String deliveryTime,
            @RequestParam(defaultValue = "PICKUP") String deliveryType,
            @RequestParam(required = false) String address,
            @RequestParam(defaultValue = "ON_DELIVERY") String paymentStatus,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "false") String militarySweets,
            RedirectAttributes redirectAttributes) {

        // Тільки залогінені
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        // Парсимо кошик
        List<Map<String, Object>> cartItems;
        try {
            cartItems = new ObjectMapper().readValue(cartJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Помилка читання кошика. Спробуйте ще раз.");
            return "redirect:/cart";
        }

        if (cartItems == null || cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Кошик порожній.");
            return "redirect:/cart";
        }

        // Перевірка: є DESIGN/AI_DESIGN без FLAVOR
        boolean hasDesign = cartItems.stream().anyMatch(i -> {
            var t = String.valueOf(i.get("type"));
            return "DESIGN".equals(t) || "AI_DESIGN".equals(t);
        });
        boolean hasFlavor = cartItems.stream()
                .anyMatch(i -> "FLAVOR".equals(String.valueOf(i.get("type"))));

        if (hasDesign && !hasFlavor) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Оберіть смак торта до дизайну!");
            return "redirect:/cart";
        }

        // Формуємо текстовий склад замовлення
        StringBuilder composition = new StringBuilder();
        int totalPrice = 0;
        String firstAiImage = null;
        String firstConstructorImg = null;
        String firstImagePath = null;   // фото першого звичайного товару
        Double totalWeight = null;

        for (Map<String, Object> item : cartItems) {
            String name     = String.valueOf(item.getOrDefault("name", ""));
            String type     = String.valueOf(item.getOrDefault("type", ""));
            Object qtyObj   = item.get("quantity");
            Object priceObj = item.get("unitPrice");
            Object discObj  = item.get("discountPrice");
            String unit     = String.valueOf(item.getOrDefault("unit", "шт"));

            double qty   = qtyObj   != null ? Double.parseDouble(String.valueOf(qtyObj))   : 1;
            double price = priceObj != null ? Double.parseDouble(String.valueOf(priceObj)) : 0;

            // Пільгова ціна якщо верифікований
            boolean isVerified = user.getVerificationStatus() != null
                    && user.getVerificationStatus().name().equals("APPROVED");
            if (isVerified && discObj != null && !discObj.toString().equals("null")) {
                price = Double.parseDouble(String.valueOf(discObj));
            }

            int subtotal = (int) Math.round(price * qty);
            totalPrice += subtotal;

            // Склад
            if (composition.length() > 0) composition.append("; ");
            composition.append(name).append(" — ").append(qty).append(" ").append(unit)
                    .append(" × ").append((int) price).append(" грн = ").append(subtotal).append(" грн");

            // Вага (тільки для FLAVOR)
            if ("FLAVOR".equals(type) && "кг".equals(unit)) {
                totalWeight = (totalWeight == null ? 0 : totalWeight) + qty;
            }

            // Зображення
            if ("AI_DESIGN".equals(type) && firstAiImage == null) {
                firstAiImage = String.valueOf(item.getOrDefault("aiImageUrl", ""));
                if (firstAiImage.isEmpty() || "null".equals(firstAiImage)) firstAiImage = null;
            } else if ("CONSTRUCTOR".equals(type) && firstConstructorImg == null) {
                firstConstructorImg = String.valueOf(item.getOrDefault("constructorImg", ""));
                if (firstConstructorImg.isEmpty() || "null".equals(firstConstructorImg)) firstConstructorImg = null;
            } else if (firstImagePath == null) {
                // Звичайний товар (FLAVOR, DESIGN, LINE) — зберігаємо фото асортименту
                String ip = String.valueOf(item.getOrDefault("imagePath", ""));
                if (!ip.isEmpty() && !"null".equals(ip)) firstImagePath = ip;
            }
        }

        // Солодощі для ЗСУ
        if ("true".equals(militarySweets)) {
            totalPrice += 100;
            if (composition.length() > 0) composition.append("; ");
            composition.append("🇺🇦 Солодощі для ЗСУ — 100 грн");
        }

        // Адреса
        String deliveryAddress;
        if ("DELIVERY".equals(deliveryType) && address != null && !address.isBlank()) {
            deliveryAddress = "Доставка: " + address.trim();
        } else {
            deliveryAddress = "Самовивіз";
        }

        // Дедлайн = дата + перший час зі слоту
        LocalDateTime deadline = null;
        if (deliveryDate != null && !deliveryDate.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(deliveryDate);
                String timeStr = (deliveryTime != null && deliveryTime.contains("-"))
                        ? deliveryTime.split("-")[0].trim() : "12:00";
                LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
                deadline = LocalDateTime.of(date, time);
            } catch (Exception ignored) {}
        }

        // Статус оплати
        Order.PaymentStatus payment;
        try {
            payment = Order.PaymentStatus.valueOf(paymentStatus);
        } catch (Exception e) {
            payment = Order.PaymentStatus.ON_DELIVERY;
        }

        // Створюємо замовлення
        Order order = new Order();
        order.setUser(user);
        order.setComposition(composition.toString());
        order.setAddress(deliveryAddress);
        order.setDeadline(deadline);
        order.setPrice(totalPrice);
        order.setWeightKg(totalWeight);
        order.setComment(comment);
        order.setAiImageUrl(firstAiImage);
        order.setConstructorImg(firstConstructorImg);
        order.setFirstImagePath(firstImagePath);
        order.setPaymentStatus(payment);
        order.setOrderStatus(Order.OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("successMsg",
                "Замовлення #" + order.getId() + " успішно оформлено! 🎂");
        return "redirect:/profile";
    }
}