package com.bakery.Bakery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("page", "orders");
        return "admin/orders";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("page", "users");
        return "admin/users";
    }

    @GetMapping("/verification")
    public String verification(Model model) {
        model.addAttribute("page", "verification");
        return "admin/verification";
    }

    @GetMapping("/assortment")
    public String assortment(Model model) {
        model.addAttribute("page", "assortment");
        return "admin/assortment";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("page", "reviews");
        return "admin/reviews";
    }
}