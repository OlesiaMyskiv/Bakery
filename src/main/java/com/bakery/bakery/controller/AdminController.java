package com.bakery.bakery.controller;

import com.bakery.bakery.model.*;
import com.bakery.bakery.repository.UserRepository;
import com.bakery.bakery.repository.OrderRepository;
import com.bakery.bakery.model.Order;
import com.bakery.bakery.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Контролер адмін-панелі.
 * Вся бізнес-логіка делегована сервісам — тут лише HTTP + Model + redirect.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final com.bakery.bakery.repository.OrderRepository orderRepository;

    public AdminController(OrderService orderService,
                           ProductService productService,
                           ReviewService reviewService,
                           UserRepository userRepository,
                           UserService userService,
                           com.bakery.bakery.repository.OrderRepository orderRepository) {
        this.orderService = orderService;
        this.productService = productService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }

    @ModelAttribute("adminUser")
    public User addAdminToModel() {
        return userService.getCurrentUser();
    }

    // ── Замовлення ────────────────────────────────────────────────────────────

    @GetMapping({"/", "/orders"})
    public String orders(@RequestParam(defaultValue = "NEW") String status, Model model) {
        Order.OrderStatus orderStatus = parseEnum(Order.OrderStatus.class, status, Order.OrderStatus.NEW);

        model.addAttribute("orders", orderService.findAllByStatus(orderStatus));
        model.addAttribute("currentStatus", orderStatus);

        // Кількість для кожного статусу — ОДИН запит через stream, не 5 запитів
        Arrays.stream(Order.OrderStatus.values()).forEach(s ->
                model.addAttribute("count_" + s.name(), orderService.countByStatus(s)));

        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestParam(defaultValue = "NEW") String currentStatus) {
        orderService.updateStatus(id, Order.OrderStatus.valueOf(status));
        return "redirect:/admin/orders?status=" + currentStatus;
    }

    // ── Клієнти ───────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q, Model model) {
        var allUsers = userRepository.findAll();

        // Пошук за ім'ям або телефоном
        if (q != null && !q.isBlank()) {
            String sq = q.trim().toLowerCase();
            allUsers = allUsers.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(sq))
                            || (u.getPhone() != null && u.getPhone().contains(sq)))
                    .toList();
        }

        model.addAttribute("users", allUsers);
        model.addAttribute("searchQuery", q);
        model.addAttribute("totalCount", userRepository.count());
        model.addAttribute("pendingCount",
                userRepository.countByVerificationStatus(VerificationStatus.PENDING));

        // Мапи для статистики замовлень
        var allOrders = orderRepository.findAll(); {

            // Кількість всіх замовлень
            java.util.Map<Long, Long> orderCountMap = allOrders.stream()
                    .filter(o -> o.getUser() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            o -> o.getUser().getId(), java.util.stream.Collectors.counting()));

            // Кількість активних замовлень (не DONE, не CANCELLED)
            java.util.Map<Long, Long> activeCountMap = allOrders.stream()
                    .filter(o -> o.getUser() != null
                            && o.getOrderStatus() != Order.OrderStatus.DONE
                            && o.getOrderStatus() != Order.OrderStatus.CANCELLED)
                    .collect(java.util.stream.Collectors.groupingBy(
                            o -> o.getUser().getId(), java.util.stream.Collectors.counting()));

            // Загальна сума покупок (тільки DONE)
            java.util.Map<Long, Integer> totalSumMap = new java.util.HashMap<>();
            allOrders.stream()
                    .filter(o -> o.getUser() != null
                            && o.getOrderStatus() == Order.OrderStatus.DONE
                            && o.getPrice() != null)
                    .forEach(o -> totalSumMap.merge(o.getUser().getId(), o.getPrice(), Integer::sum));

            model.addAttribute("orderCountMap",  orderCountMap);
            model.addAttribute("activeCountMap",  activeCountMap);
            model.addAttribute("totalSumMap",     totalSumMap);
        }

        return "admin/users";
    }

    // ══════════════════════════════════════════════════════════════════════════════
// ВСТАВИТИ В AdminController.java після методу users()
// Також додати імпорти якщо відсутні:
//   import org.springframework.http.ResponseEntity;
//   import org.springframework.web.bind.annotation.ResponseBody;
// ══════════════════════════════════════════════════════════════════════════════

    @GetMapping("/users/{id}/history")
    @ResponseBody
    public ResponseEntity<?> userHistory(@PathVariable Long id) {
        // Використовуємо orderService а не orderRepository
        var orders = orderService.findAllByUser(id);

        var result = orders.stream().map(o -> {
            var m = new java.util.HashMap<String, Object>();
            m.put("id",          o.getId());
            m.put("composition", o.getComposition() != null ? o.getComposition() : "—");
            m.put("status",      o.getOrderStatus().getLabel());
            m.put("statusKey",   o.getOrderStatus().name());
            m.put("deadline",    o.getDeadline() != null
                    ? o.getDeadline().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    : "—");
            m.put("createdAt",   o.getCreatedAt() != null
                    ? o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : "—");
            m.put("price", o.getPrice() != null ? o.getPrice() + " грн" : "—");
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }
    // ── Верифікація ───────────────────────────────────────────────────────────

    @GetMapping("/verification")
    public String verification(Model model) {
        model.addAttribute("pendingUsers",
                userRepository.findByVerificationStatusOrderByIdDesc(VerificationStatus.PENDING));
        model.addAttribute("approvedUsers",
                userRepository.findByVerificationStatusOrderByIdDesc(VerificationStatus.APPROVED));
        model.addAttribute("pendingCount",
                userRepository.countByVerificationStatus(VerificationStatus.PENDING));
        return "admin/verification";
    }

    @PostMapping("/verification/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setVerificationStatus(VerificationStatus.APPROVED);
            deleteDocumentFile(u);
            u.setDocumentPath(null);
            userRepository.save(u);
        });
        return "redirect:/admin/verification";
    }

    @PostMapping("/verification/{id}/reject")
    public String rejectUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setVerificationStatus(VerificationStatus.REJECTED);
            u.setRole(Role.CLIENT);
            deleteDocumentFile(u);
            u.setDocumentPath(null);
            userRepository.save(u);
        });
        return "redirect:/admin/verification";
    }

    private void deleteDocumentFile(User u) {
        if (u.getDocumentPath() == null) return;
        try {
            java.nio.file.Path filePath = java.nio.file.Paths
                    .get(System.getProperty("user.dir"))
                    .resolve(u.getDocumentPath().replaceFirst("^/", ""));
            java.nio.file.Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}
    }

    // ── Асортимент ────────────────────────────────────────────────────────────

    @GetMapping("/assortment")
    public String assortment(@RequestParam(defaultValue = "FLAVOR") String catalog, Model model) {
        Product.CatalogType catalogType = parseEnum(
                Product.CatalogType.class, catalog, Product.CatalogType.FLAVOR);

        model.addAttribute("products", productService.findByCatalogAdmin(catalogType));
        model.addAttribute("currentCatalog", catalogType);
        model.addAttribute("catalogTypes", Product.CatalogType.values());
        model.addAttribute("flavorBases",  Product.FlavorBase.values());
        model.addAttribute("designEvents", Product.DesignEvent.values());
        model.addAttribute("designFors",   Product.DesignFor.values());
        model.addAttribute("productLines", Product.ProductLine.values());
        model.addAttribute("urgencies",    Product.Urgency.values());
        return "admin/assortment";
    }

    @PostMapping("/assortment/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam String catalogType,
            @RequestParam(required = false) String flavorBase,
            @RequestParam(required = false) List<String> dietaryTags,
            @RequestParam(required = false) String designEvent,
            @RequestParam(required = false) List<String> designFor,
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) String urgency,
            @RequestParam Integer price,
            @RequestParam String priceUnit,
            @RequestParam(required = false) String description,
            // ── НОВІ ПАРАМЕТРИ ────────────────────────────────────────────────
            @RequestParam(required = false, defaultValue = "0") Integer discountPercent,
            @RequestParam(required = false) String flavorDescription,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String calories,
            @RequestParam(required = false) String allergens,
            @RequestParam(required = false, defaultValue = "1.0") Double minWeightKg,
            // ─────────────────────────────────────────────────────────────────
            @RequestParam(required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) throws IOException {

        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setPriceUnit(priceUnit);
        p.setDescription(description);
        p.setCatalogType(Product.CatalogType.valueOf(catalogType));

        // Нові поля
        p.setDiscountPercent(discountPercent != null ? discountPercent : 0);
        p.setFlavorDescription(flavorDescription);
        p.setIngredients(ingredients);
        p.setCalories(calories);
        p.setAllergens(allergens);
        p.setMinWeightKg(minWeightKg != null ? minWeightKg : 1.0);

        if (flavorBase  != null && !flavorBase.isEmpty())
            p.setFlavorBase(Product.FlavorBase.valueOf(flavorBase));
        if (dietaryTags != null && !dietaryTags.isEmpty())
            p.setDietaryTags(String.join(",", dietaryTags));
        if (designEvent != null && !designEvent.isEmpty())
            p.setDesignEvent(Product.DesignEvent.valueOf(designEvent));
        if (designFor   != null && !designFor.isEmpty())
            p.setDesignFor(String.join(",", designFor));
        if (productLine != null && !productLine.isEmpty())
            p.setProductLine(Product.ProductLine.valueOf(productLine));
        if (urgency     != null && !urgency.isEmpty())
            p.setUrgency(Product.Urgency.valueOf(urgency));

        productService.save(p, image);
        redirectAttributes.addFlashAttribute("successMsg", "Продукт \"" + name + "\" додано.");
        return "redirect:/admin/assortment?catalog=" + catalogType;
    }



    @PostMapping("/assortment/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @RequestParam(defaultValue = "FLAVOR") String catalog) {
        productService.delete(id);
        return "redirect:/admin/assortment?catalog=" + catalog;
    }

    @PostMapping("/assortment/{id}/toggle")
    public String toggleProduct(@PathVariable Long id,
                                @RequestParam(defaultValue = "FLAVOR") String catalog) {
        productService.toggleAvailability(id);
        return "redirect:/admin/assortment?catalog=" + catalog;
    }
    /**
     * GET /admin/assortment/{id}/edit
     * Відкриває сторінку редагування товару з заповненими полями.
     */
    @GetMapping("/assortment/{id}/edit")
    public String editProductForm(@PathVariable Long id,
                                  @RequestParam(defaultValue = "FLAVOR") String catalog,
                                  Model model) {
        Product product = productService.findByIdOrThrow(id);

        model.addAttribute("product",      product);
        model.addAttribute("catalog",      catalog);
        model.addAttribute("catalogTypes", Product.CatalogType.values());
        model.addAttribute("flavorBases",  Product.FlavorBase.values());
        model.addAttribute("designEvents", Product.DesignEvent.values());
        model.addAttribute("designFors",   Product.DesignFor.values());
        model.addAttribute("productLines", Product.ProductLine.values());
        model.addAttribute("urgencies",    Product.Urgency.values());
        model.addAttribute("adminUser",    userService.getCurrentUser());
        return "admin/edit-product";
    }

    /**
     * POST /admin/assortment/{id}/edit
     * Зберігає зміни товару.
     */
    @PostMapping("/assortment/{id}/edit")
    public String editProductSave(
            @PathVariable Long id,
            @RequestParam String catalog,
            @RequestParam String name,
            @RequestParam Integer price,
            @RequestParam String priceUnit,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String flavorBase,
            @RequestParam(required = false) List<String> dietaryTags,
            @RequestParam(required = false) String designEvent,
            @RequestParam(required = false) List<String> designFor,
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false, defaultValue = "0") Integer discountPercent,
            @RequestParam(required = false) String flavorDescription,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String calories,
            @RequestParam(required = false) String allergens,
            @RequestParam(required = false) Double minWeightKg,
            @RequestParam(required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) throws IOException {

        Product p = productService.findByIdOrThrow(id);

        // Основні поля
        p.setName(name);
        p.setPrice(price);
        p.setPriceUnit(priceUnit);
        p.setDescription(description);
        p.setDiscountPercent(discountPercent != null ? discountPercent : 0);
        p.setFlavorDescription(flavorDescription);
        p.setIngredients(ingredients);
        p.setCalories(calories);
        p.setAllergens(allergens);
        p.setMinWeightKg(minWeightKg);

        // Enum поля
        p.setFlavorBase(flavorBase != null && !flavorBase.isEmpty()
                ? Product.FlavorBase.valueOf(flavorBase) : null);
        p.setDesignEvent(designEvent != null && !designEvent.isEmpty()
                ? Product.DesignEvent.valueOf(designEvent) : null);
        p.setProductLine(productLine != null && !productLine.isEmpty()
                ? Product.ProductLine.valueOf(productLine) : null);
        p.setUrgency(urgency != null && !urgency.isEmpty()
                ? Product.Urgency.valueOf(urgency) : null);

        // Списки
        p.setDietaryTags(dietaryTags != null && !dietaryTags.isEmpty()
                ? String.join(",", dietaryTags) : null);
        p.setDesignFor(designFor != null && !designFor.isEmpty()
                ? String.join(",", designFor) : null);

        // Фото — замінюємо тільки якщо нове завантажено
        productService.save(p, (image != null && !image.isEmpty()) ? image : null);

        redirectAttributes.addFlashAttribute("successMsg",
                "Товар \"" + name + "\" успішно оновлено!");
        return "redirect:/admin/assortment?catalog=" + catalog;
    }


    // ── Відгуки ───────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<Review> list = switch (filter) {
            case "unanswered" -> reviewService.findUnanswered();
            case "hidden"     -> reviewService.findHidden();
            default           -> reviewService.findVisible();
        };
        model.addAttribute("reviews", list);
        model.addAttribute("filter", filter);
        model.addAttribute("unansweredCount", reviewService.countUnanswered());
        model.addAttribute("totalCount", reviewService.findVisible().size());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyToReview(@PathVariable Long id,
                                @RequestParam String reply,
                                @RequestParam(defaultValue = "all") String filter) {
        reviewService.reply(id, reply);
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @PostMapping("/reviews/{id}/hide")
    public String hideReview(@PathVariable Long id,
                             @RequestParam(defaultValue = "all") String filter) {
        reviewService.toggleVisibility(id);
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam(defaultValue = "all") String filter) {
        reviewService.deleteById(id);
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @GetMapping("/orders/{id}/print")
    public String printOrder(@PathVariable Long id, Model model) {
        Order order = orderRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("Замовлення #" + id + " не знайдено"));
        model.addAttribute("order", order);
        return "admin/order-print";
    }

    @GetMapping("/chats")
    public String chatsPage() {
        return "admin/chats";
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String raw, E fallback) {
        try { return Enum.valueOf(clazz, raw); }
        catch (Exception e) { return fallback; }
    }
}