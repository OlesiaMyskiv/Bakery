package com.bakery.Bakery.controller;

import com.bakery.Bakery.service.AiImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AiDesignController {

    @Autowired
    private AiImageService aiImageService;

    // Відкриває саму сторінку
    @GetMapping("/ai-design")
    public String getAiDesignPage() {
        return "ai-design"; // HTML шаблон
    }

    // Приймає дані з форми і повертає картинку
    @PostMapping("/api/generate-design")
    @ResponseBody
    public Map<String, String> generateDesignEndpoint(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String occasion = payload.get("occasion");
        String colors = payload.get("colors");

        // Об'єднуємо всі побажання клієнта в один запит
        String fullPrompt = String.format("Подія: %s. Кольорова гама: %s. Опис від клієнта: %s", occasion, colors, prompt);

        String imageUrl = aiImageService.generateCakeDesign(fullPrompt);

        Map<String, String> response = new HashMap<>();
        if (imageUrl != null) {
            response.put("imageUrl", imageUrl);
        } else {
            response.put("error", "Сталася помилка при генерації. Перевірте API-ключ.");
        }
        return response;
    }
}