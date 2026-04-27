package com.bakery.Bakery.controller;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/profile")
    public String profilePage(jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";
        return "profile"; // Відкриває сторінку "Активні замовлення"
    }

    @GetMapping("/constructor")
    public String constructorPage() {
        return "constructor";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        // Тут можна додати логіку отримання даних поточного користувача
        return "profile";
    }
}