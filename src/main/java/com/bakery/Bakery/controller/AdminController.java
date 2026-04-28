package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository;

    // =============================================
    // ЗАМОВЛЕННЯ
    // =============================================

    @GetMapping("/admin")
    public String adminHome(Model model) {
        return ordersPage("NEW", model);
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "NEW") String status, Model model) {
        return ordersPage(status, model);
    }

    private String ordersPage(String status, Model model) {
        Orderі.OrderStatus orderStatus;
        try {
            orderStatus = Orderі.OrderStatus.valueOf(status);
        } catch (Exception e) {
            orderStatus = Orderі.OrderStatus.NEW;
        }

        model.addAttribute("orders", orderRepository.findByOrderStatusOrderByCreatedAtDesc(orderStatus));
        model.addAttribute("currentStatus", orderStatus);
        model.addAttribute("allStatuses", Orderі.OrderStatus.values());

        // лічильники для вкладок
        for (Orderі.OrderStatus s : Orderі.OrderStatus.values()) {
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
            order.setOrderStatus(Orderі.OrderStatus.valueOf(status));
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
    public String assortment(@RequestParam(required = false) String type, Model model) {
        if (type != null && !type.isEmpty()) {
            try {
                model.addAttribute("products",
                    productRepository.findByTypeOrderByNameAsc(Product.ProductType.valueOf(type)));
                model.addAttribute("currentType", type);
            } catch (Exception e) {
                model.addAttribute("products", productRepository.findAllByOrderByTypeAscNameAsc());
            }
        } else {
            model.addAttribute("products", productRepository.findAllByOrderByTypeAscNameAsc());
            model.addAttribute("currentType", "ALL");
        }
        model.addAttribute("productTypes", Product.ProductType.values());
        return "admin/assortment";
    }

    @PostMapping("/assortment/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam Integer price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile image) throws IOException {

        Product product = new Product();
        product.setName(name);
        product.setType(Product.ProductType.valueOf(type));
        product.setPricePerKg(price);
        product.setDescription(description);

        if (image != null && !image.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/products";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Files.write(uploadPath.resolve(fileName), image.getBytes());
            product.setImagePath("/uploads/products/" + fileName);
        }

        productRepository.save(product);
        return "redirect:/admin/assortment";
    }

    @PostMapping("/assortment/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return "redirect:/admin/assortment";
    }

    @PostMapping("/assortment/{id}/toggle")
    public String toggleProduct(@PathVariable Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setAvailable(!p.isAvailable());
            productRepository.save(p);
        });
        return "redirect:/admin/assortment";
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
                        .stream().filter(r -> r.isHidden()).toList());
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
