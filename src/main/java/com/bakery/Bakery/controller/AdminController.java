package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.UserRepository;
import com.bakery.Bakery.service.*;
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

    public AdminController(OrderService orderService,
                           ProductService productService,
                           ReviewService reviewService,
                           UserRepository userRepository,
                           UserService userService) {
        this.orderService = orderService;
        this.productService = productService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.userService = userService;
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
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("totalCount", userRepository.count());
        model.addAttribute("pendingCount",
                userRepository.countByVerificationStatus(VerificationStatus.PENDING));
        return "admin/users";
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
            userRepository.save(u);
        });
        return "redirect:/admin/verification";
    }

    @PostMapping("/verification/{id}/reject")
    public String rejectUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setVerificationStatus(VerificationStatus.REJECTED);
            userRepository.save(u);
        });
        return "redirect:/admin/verification";
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
            @RequestParam(required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) throws IOException {

        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setPriceUnit(priceUnit);
        p.setDescription(description);
        p.setCatalogType(Product.CatalogType.valueOf(catalogType));

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