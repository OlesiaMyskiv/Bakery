package com.bakery.Bakery.controller;

// ДОДАНО ДВА НОВИХ ІМПОРТИ:
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;

@Controller
public class HomeController {

    // ДОДАНО ІН'ЄКЦІЮ РЕПОЗИТОРІЮ:
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/profile")
    public String showProfilePage(Model model) {
        // Отримуємо поточного авторизованого користувача
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName(); // Spring зберігає email як логін

            // Знаходимо користувача в базі даних (тепер userRepository працюватиме!)
            com.bakery.Bakery.model.User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                model.addAttribute("user", user); // Передаємо об'єкт user у файл profile.html
                return "profile";
            }
        }

        // Якщо не авторизований або сталася помилка — на логін
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/constructor")
    public String constructorPage() {
        return "constructor";
    }
}