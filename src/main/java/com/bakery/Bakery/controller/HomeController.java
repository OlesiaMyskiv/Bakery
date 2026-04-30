package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Product;
import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.repository.ProductRepository;
import com.bakery.Bakery.repository.ReviewRepository;
import com.bakery.Bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository  reviewRepository;
    @Autowired private UserService       userService;

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/constructor")
    public String constructorPage() { return "constructor"; }

    @GetMapping("/product/{id}")
    public String productPage(@PathVariable Long id, Model model) {

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return "redirect:/assortment";

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewRepository.findByProductIdAndHiddenFalse(id));

        // ── Чи юзер верифікований ─────────────────────────────────────────────
        User currentUser = userService.getCurrentUser();
        boolean isVerified = currentUser != null
                && currentUser.getVerificationStatus() == VerificationStatus.APPROVED
                && currentUser.getRole() != Role.SUPER_ADMIN;
        model.addAttribute("isVerified", isVerified);

        // ── Breadcrumb ────────────────────────────────────────────────────────
        String catalogLabel = product.getCatalogType() != null
                ? product.getCatalogType().getLabel() : "Асортимент";
        String catalogParam = product.getCatalogType() != null
                ? product.getCatalogType().name() : "FLAVOR";
        model.addAttribute("catalogLabel", catalogLabel);
        model.addAttribute("catalogParam", catalogParam);

        // ── Рекомендовані — залежать від типу товару ──────────────────────────
        List<Product> recommended = getRecommended(product, id);
        model.addAttribute("recommendedProducts", recommended);

        return "product-detail";
    }

    /**
     * FLAVOR → рекомендуємо 5 рандомних DESIGN
     * DESIGN → рекомендуємо 5 рандомних FLAVOR
     * LINE   → рекомендуємо 5 рандомних з тієї ж LINE (або будь-які LINE якщо мало)
     */
    private List<Product> getRecommended(Product product, Long excludeId) {
        if (product.getCatalogType() == null) return List.of();

        Product.CatalogType targetType = switch (product.getCatalogType()) {
            case FLAVOR -> Product.CatalogType.DESIGN;
            case DESIGN -> Product.CatalogType.FLAVOR;
            case LINE   -> Product.CatalogType.LINE;
        };

        List<Product> pool = new ArrayList<>(
                productRepository.findByCatalogTypeAndAvailableTrueOrderByNameAsc(targetType)
        );

        // Виключаємо поточний товар
        pool.removeIf(p -> p.getId().equals(excludeId));

        Collections.shuffle(pool, new Random());
        return pool.stream().limit(5).toList();
    }

}