package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Product;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.repository.ProductRepository;
import com.bakery.Bakery.repository.ReviewRepository;
import com.bakery.Bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Random;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserService userService;

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

        // ── Чи юзер верифікований (бачить пільгову ціну) ─────────────────────
        User currentUser = userService.getCurrentUser();
        boolean isVerified = currentUser != null
                && currentUser.getVerificationStatus() == VerificationStatus.APPROVED
                && currentUser.getRole() != Role.SUPER_ADMIN;
        model.addAttribute("isVerified", isVerified);

        // ── Рекомендовані дизайни — 5 рандомних з DESIGN каталогу ────────────
        List<Product> allDesigns = productRepository
                .findByCatalogTypeAndAvailableTrueOrderByNameAsc(Product.CatalogType.DESIGN);

        // Виключаємо поточний товар і беремо 5 рандомних
        List<Product> filteredDesigns = allDesigns.stream()
                .filter(p -> !p.getId().equals(id))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        java.util.Collections.shuffle(filteredDesigns, new Random());
        List<Product> recommended = filteredDesigns.stream().limit(5).toList();
        model.addAttribute("recommendedDesigns", recommended);

        // ── Breadcrumb — визначаємо каталог ──────────────────────────────────
        String catalogLabel = product.getCatalogType() != null
                ? product.getCatalogType().getLabel() : "Асортимент";
        String catalogParam = product.getCatalogType() != null
                ? product.getCatalogType().name() : "FLAVOR";
        model.addAttribute("catalogLabel", catalogLabel);
        model.addAttribute("catalogParam", catalogParam);

        return "product-detail";
    }
}