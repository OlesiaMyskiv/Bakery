package com.bakery.bakery.controller;

import com.bakery.bakery.model.Product;
import com.bakery.bakery.model.Role;
import com.bakery.bakery.model.User;
import com.bakery.bakery.model.VerificationStatus;
import com.bakery.bakery.repository.ProductRepository;
import com.bakery.bakery.repository.OrderRepository;
import com.bakery.bakery.repository.BonusTransactionRepository;
import com.bakery.bakery.repository.ReviewRepository;
import com.bakery.bakery.service.UserService;
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
    @Autowired private OrderRepository              orderRepository;
    @Autowired private BonusTransactionRepository    bonusTransactionRepository;
    private static final int MILITARY_GOAL = 50;
    @Autowired private ReviewRepository  reviewRepository;
    @Autowired private UserService       userService;

    @GetMapping("/")
    public String home(Model model) {
        // Популярні товари — 3 рандомних FLAVOR
        List<Product> all = new ArrayList<>(
                productRepository.findByCatalogTypeAndAvailableTrueOrderByNameAsc(Product.CatalogType.FLAVOR)
        );
        Collections.shuffle(all, new Random());
        model.addAttribute("popularProducts", all.stream().limit(3).toList());

        // Відгуки для головної — 3 останніх
        model.addAttribute("homeReviews", reviewRepository.findTop3ByHiddenFalseOrderByCreatedAtDesc());

        // Лічильник солодощів для ЗСУ — з БД
        long militaryCount = orderRepository.countMilitarySweetsThisMonth();
        model.addAttribute("militaryCount", militaryCount);
        model.addAttribute("militaryGoal",  MILITARY_GOAL);
        model.addAttribute("militaryPct",   Math.min(Math.round(militaryCount * 100.0 / MILITARY_GOAL), 100));

        // ТОП донаторів місяця
        List<Object[]> rawTop = bonusTransactionRepository.findTopDonatorsByMonth();
        java.util.List<java.util.Map<String, Object>> topDonators = new java.util.ArrayList<>();
        for (int i = 0; i < rawTop.size(); i++) {
            Object[] row = rawTop.get(i);
            java.util.Map<String, Object> d = new java.util.HashMap<>();
            d.put("rank",    i + 1);
            d.put("name",    row[1] != null ? row[1].toString() : "Анонім");
            d.put("photo",   row[2] != null ? row[2].toString() : null);
            d.put("total",   row[3] != null ? ((Number) row[3]).intValue() : 0);
            topDonators.add(d);
        }
        model.addAttribute("topDonators", topDonators);

        return "index";
    }

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
        boolean isLoggedIn = currentUser != null;
        model.addAttribute("isVerified",  isVerified);
        model.addAttribute("isLoggedIn",  isLoggedIn);

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