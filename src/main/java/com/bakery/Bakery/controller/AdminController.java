package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository;

    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // =============================================
    // ЗАМОВЛЕННЯ
    // =============================================

    @GetMapping({"/admin", "/orders"})
    public String orders(@RequestParam(defaultValue = "NEW") String status, Model model) {
        Order.OrderStatus orderStatus;
        try {
            orderStatus = Order.OrderStatus.valueOf(status);
        } catch (Exception e) {
            orderStatus = Order.OrderStatus.NEW;
        }
        model.addAttribute("orders",
                orderRepository.findByOrderStatusOrderByCreatedAtDesc(orderStatus));
        model.addAttribute("currentStatus", orderStatus);
        model.addAttribute("adminUser", getCurrentAdmin());
        for (Order.OrderStatus s : Order.OrderStatus.values()) {
            model.addAttribute("count_" + s.name(),
                    orderRepository.findByOrderStatusOrderByCreatedAtDesc(s).size());
        }
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestParam(defaultValue = "NEW") String currentStatus) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setOrderStatus(Order.OrderStatus.valueOf(status));
            orderRepository.save(order);
        });
        return "redirect:/admin/orders?status=" + currentStatus;
    }

    // =============================================
    // КЛІЄНТИ
    // =============================================

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("totalCount", userRepository.count());
        model.addAttribute("pendingCount",
                userRepository.countByVerificationStatus(VerificationStatus.PENDING));
        model.addAttribute("adminUser", getCurrentAdmin());
        return "admin/users";
    }

    // =============================================
    // ВЕРИФІКАЦІЯ
    // =============================================

    @GetMapping("/verification")
    public String verification(Model model) {
        model.addAttribute("pendingUsers",
                userRepository.findByVerificationStatusOrderByIdDesc(VerificationStatus.PENDING));
        model.addAttribute("approvedUsers",
                userRepository.findByVerificationStatusOrderByIdDesc(VerificationStatus.APPROVED));
        model.addAttribute("pendingCount",
                userRepository.countByVerificationStatus(VerificationStatus.PENDING));
        model.addAttribute("adminUser", getCurrentAdmin());
        return "admin/verification";
    }

    @PostMapping("/verification/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setVerificationStatus(VerificationStatus.APPROVED);
            userRepository.save(user);
        });
        return "redirect:/admin/verification";
    }

    @PostMapping("/verification/{id}/reject")
    public String rejectUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setVerificationStatus(VerificationStatus.REJECTED);
            userRepository.save(user);
        });
        return "redirect:/admin/verification";
    }

    // =============================================
    // АСОРТИМЕНТ
    // =============================================

    @GetMapping("/assortment")
    public String assortment(
            @RequestParam(defaultValue = "FLAVOR") String catalog,
            Model model) {

        Product.CatalogType catalogType;
        try {
            catalogType = Product.CatalogType.valueOf(catalog);
        } catch (Exception e) {
            catalogType = Product.CatalogType.FLAVOR;
        }

        // Товари поточного каталогу (для адміна — всі, включно з прихованими)
        model.addAttribute("products",
                productRepository.findByCatalogTypeOrderByNameAsc(catalogType));
        model.addAttribute("currentCatalog", catalogType);
        model.addAttribute("catalogTypes", Product.CatalogType.values());

        // Enum-и для форми
        model.addAttribute("flavorBases",   Product.FlavorBase.values());
        model.addAttribute("designEvents",  Product.DesignEvent.values());
        model.addAttribute("productLines",  Product.ProductLine.values());
        model.addAttribute("urgencies",     Product.Urgency.values());

        model.addAttribute("adminUser", getCurrentAdmin());
        return "admin/assortment";
    }

    @PostMapping("/assortment/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam String catalogType,
            @RequestParam(required = false) String flavorBase,
            @RequestParam(required = false) List<String> dietaryTags,
            @RequestParam(required = false) String designEvent,
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) String urgency,
            @RequestParam Integer price,
            @RequestParam String priceUnit,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile image) throws IOException {

        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setPriceUnit(priceUnit);
        product.setDescription(description);

        Product.CatalogType catalog = Product.CatalogType.valueOf(catalogType);
        product.setCatalogType(catalog);

        if (flavorBase != null && !flavorBase.isEmpty())
            product.setFlavorBase(Product.FlavorBase.valueOf(flavorBase));

        if (dietaryTags != null && !dietaryTags.isEmpty())
            product.setDietaryTags(String.join(",", dietaryTags));

        if (designEvent != null && !designEvent.isEmpty())
            product.setDesignEvent(Product.DesignEvent.valueOf(designEvent));

        if (productLine != null && !productLine.isEmpty())
            product.setProductLine(Product.ProductLine.valueOf(productLine));

        if (urgency != null && !urgency.isEmpty())
            product.setUrgency(Product.Urgency.valueOf(urgency));

        // ============================================================
        // ЗБЕРЕЖЕННЯ ФОТО В ОКРЕМУ ПАПКУ ЗАЛЕЖНО ВІД КАТАЛОГУ
        // Структура: uploads/assortment/flavor/   ← Каталог Смаків
        //            uploads/assortment/design/   ← Каталог Дизайнів
        //            uploads/assortment/line/     ← Лінійка виробів
        // ============================================================
        if (image != null && !image.isEmpty()) {
            // Визначаємо підпапку
            String subFolder = switch (catalog) {
                case FLAVOR -> "flavor";
                case DESIGN -> "design";
                case LINE   -> "line";
            };

            String uploadDir = System.getProperty("user.dir")
                    + "/uploads/assortment/" + subFolder;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Files.write(uploadPath.resolve(fileName), image.getBytes());

            // Шлях який збережеться в БД і використовується в <img src="...">
            product.setImagePath("/uploads/assortment/" + subFolder + "/" + fileName);
        }

        productRepository.save(product);
        return "redirect:/admin/assortment?catalog=" + catalogType;
    }

    @PostMapping("/assortment/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @RequestParam(defaultValue = "FLAVOR") String catalog) {
        productRepository.deleteById(id);
        return "redirect:/admin/assortment?catalog=" + catalog;
    }

    @PostMapping("/assortment/{id}/toggle")
    public String toggleProduct(@PathVariable Long id,
                                @RequestParam(defaultValue = "FLAVOR") String catalog) {
        productRepository.findById(id).ifPresent(p -> {
            p.setAvailable(!p.isAvailable());
            productRepository.save(p);
        });
        return "redirect:/admin/assortment?catalog=" + catalog;
    }

    // =============================================
    // ВІДГУКИ
    // =============================================

    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "all") String filter, Model model) {
        switch (filter) {
            case "unanswered":
                model.addAttribute("reviews",
                        reviewRepository.findByAdminReplyIsNullAndHiddenFalseOrderByCreatedAtDesc());
                break;
            case "hidden":
                model.addAttribute("reviews",
                        reviewRepository.findAllByOrderByCreatedAtDesc()
                                .stream().filter(Review::isHidden).toList());
                break;
            default:
                model.addAttribute("reviews",
                        reviewRepository.findByHiddenFalseOrderByCreatedAtDesc());
        }
        model.addAttribute("filter", filter);
        model.addAttribute("unansweredCount",
                reviewRepository.countByAdminReplyIsNullAndHiddenFalse());
        model.addAttribute("totalCount",
                reviewRepository.findByHiddenFalseOrderByCreatedAtDesc().size());
        model.addAttribute("adminUser", getCurrentAdmin());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyToReview(@PathVariable Long id,
                                @RequestParam String reply,
                                @RequestParam(defaultValue = "all") String filter) {
        reviewRepository.findById(id).ifPresent(review -> {
            review.setAdminReply(reply);
            reviewRepository.save(review);
        });
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @PostMapping("/reviews/{id}/hide")
    public String hideReview(@PathVariable Long id,
                             @RequestParam(defaultValue = "all") String filter) {
        reviewRepository.findById(id).ifPresent(review -> {
            review.setHidden(!review.isHidden());
            reviewRepository.save(review);
        });
        return "redirect:/admin/reviews?filter=" + filter;
    }
}