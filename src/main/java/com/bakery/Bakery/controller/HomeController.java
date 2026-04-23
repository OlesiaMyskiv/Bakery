package com.bakery.Bakery.controller;

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

}