package com.bakery.Bakery.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.bakery.Bakery.service.AiImageService;

@Controller
public class AiDesignController {

    private final AiImageService aiImageService;

    public AiDesignController(AiImageService aiImageService) {
        this.aiImageService = aiImageService;
    }

    @GetMapping("/ai-design")
    public String showAiDesignPage() {
        return "ai-design";
    }

    @GetMapping("/ai-design/generate")
    public String redirectGenerate() {
        return "redirect:/ai-design";
    }

    @PostMapping("/ai-design/generate")
    public String generate(@RequestParam("prompt") String prompt, Model model) {
        String imageUrl = aiImageService.generateCakeImage(prompt);
        model.addAttribute("generatedImageUrl", imageUrl);
        model.addAttribute("userPrompt", prompt);
        return "ai-design";
    }
}