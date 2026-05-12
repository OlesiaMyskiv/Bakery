package com.bakery.bakery.controller;

import com.bakery.bakery.model.Order;
import com.bakery.bakery.model.User;
import com.bakery.bakery.repository.OrderRepository;
import com.bakery.bakery.service.UserService;
import com.bakery.bakery.service.BonusService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired private UserService userService;
    @Autowired private OrderRepository orderRepository;
    @Autowired(required = false) private BonusService bonusService;

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
            @RequestParam(defaultValue = "0") int useBonuses,
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
        java.util.List<String> itemImages = new ArrayList<>(); // фото кожного товару
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
            if (composition.length() > 0) composition.append(" | ");
            composition.append(name).append(" — ").append(qty).append(" ").append(unit)
                    .append(" × ").append((int) price).append(" грн = ").append(subtotal).append(" грн");

            // Для авторського торту (CONSTRUCTOR) — додаємо деталі складу через ";;"
            if ("CONSTRUCTOR".equals(type)) {
                String desc = String.valueOf(item.getOrDefault("description", ""));
                if (!desc.isEmpty() && !"null".equals(desc)) {
                    // Замінюємо " | " між шарами на ";;" щоб не плутати з роздільником товарів
                    composition.append(";;").append(desc.replace(" | ", ";;"));
                }
            }

            // Вага тільки для FLAVOR (кг), не для конструктора чи дизайнів
            if ("FLAVOR".equals(type) && "кг".equals(unit)) {
                totalWeight = (totalWeight == null ? 0 : totalWeight) + qty;
            }
            // CONSTRUCTOR — вага не рахується (це шт, не кг)

            // Зображення — зберігаємо для кожного товару
            String itemImg = "/img/2.png"; // дефолт
            if ("AI_DESIGN".equals(type)) {
                String ai = String.valueOf(item.getOrDefault("aiImageUrl", ""));
                if (!ai.isEmpty() && !"null".equals(ai)) { itemImg = ai; if (firstAiImage == null) firstAiImage = ai; }
            } else if ("CONSTRUCTOR".equals(type)) {
                String ci = String.valueOf(item.getOrDefault("constructorImg", ""));
                if (!ci.isEmpty() && !"null".equals(ci)) { itemImg = ci; if (firstConstructorImg == null) firstConstructorImg = ci; }
            } else if (name.contains("Солодощі для ЗСУ") || name.contains("🇺🇦")) {
                itemImg = "/img/energy_ua.png";
                if (firstImagePath == null) firstImagePath = "/img/energy_ua.png";
            } else {
                String ip = String.valueOf(item.getOrDefault("imagePath", ""));
                if (!ip.isEmpty() && !"null".equals(ip)) { itemImg = ip; if (firstImagePath == null) firstImagePath = ip; }
            }
            itemImages.add(itemImg);
        }

        // Солодощі для ЗСУ
        if ("true".equals(militarySweets)) {
            totalPrice += 100;
            if (composition.length() > 0) composition.append(" | ");
            composition.append("🇺🇦 Солодощі для ЗСУ — 100 грн");
            itemImages.add("/img/energy_ua.png");
            if (firstImagePath == null) firstImagePath = "/img/energy_ua.png";
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
        // Зберігаємо JSON масив фото всіх товарів
        try {
            order.setImagesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(itemImages));
        } catch (Exception e) { order.setImagesJson("[]"); }
        order.setPaymentStatus(payment);
        order.setOrderStatus(Order.OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        // Списуємо бонуси якщо клієнт обрав
        if (useBonuses > 0 && bonusService != null) {
            bonusService.spend(user, order, useBonuses);
        }

        redirectAttributes.addFlashAttribute("successMsg",
                "Замовлення #" + order.getId() + " успішно оформлено! 🎂");
        return "redirect:/profile";
    }
}