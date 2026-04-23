package com.bakery.Bakery.controller;

// 1. ВИПРАВЛЕНО: Правильний імпорт
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Імпортуємо наш сервіс (переконайтеся, що шлях правильний)
import com.bakery.Bakery.service.AiImageService;

@Controller
public class AiDesignController {

    // 2. ВИПРАВЛЕНО: Додаємо сервіс у контролер
    private final AiImageService aiImageService;

    // Spring автоматично "інжектить" (підставить) цей сервіс сюди
    public AiDesignController(AiImageService aiImageService) {
        this.aiImageService = aiImageService;
    }

    @GetMapping("/ai-design")
    public String showAiDesignPage() {
        return "ai-desig"; // назва вашого HTML файлу (у вас він названий ai-desig)
    }

    @PostMapping("/ai-design/generate")
    public String generate(@RequestParam("prompt") String prompt, Model model) {
        // Тепер aiImageService працює!
        String imageUrl = aiImageService.generateCakeImage(prompt);
        model.addAttribute("generatedImageUrl", imageUrl);
        model.addAttribute("userPrompt", prompt);
        return "ai-desig";
    }
}