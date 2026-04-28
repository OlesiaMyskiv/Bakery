package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Order;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.OrderRepository;
import com.bakery.Bakery.repository.ProductRepository;
import com.bakery.Bakery.repository.ReviewRepository;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ---- Головна сторінка ----
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // ---- Логін ----
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ---- Профіль ----
    @GetMapping("/profile")
    public String showProfilePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                model.addAttribute("user", user);
                return "profile";
            }
        }
        return "redirect:/login";
    }

    // ---- Відновлення пароля ----
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // ---- Реєстрація ----
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ---- Конструктор ----
    @GetMapping("/constructor")
    public String constructorPage() {
        return "constructor";
    }

    // ---- Сторінка окремого товару ----
    @GetMapping("/product/{id}")
    public String productPage(@PathVariable Long id, Model model) {
        productRepository.findById(id).ifPresent(product -> {
            model.addAttribute("product", product);
            model.addAttribute("reviews",
                    reviewRepository.findByProductIdAndHiddenFalse(id));
        });

        // Якщо товар не знайдено — повертаємо на асортимент
        if (!model.containsAttribute("product") ||
                model.getAttribute("product") == null) {
            return "redirect:/assortment";
        }

        return "product-detail";
    }
    @GetMapping("/gen-password")
    @ResponseBody
    public String genPass() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return encoder.encode("19102005");
    }
}