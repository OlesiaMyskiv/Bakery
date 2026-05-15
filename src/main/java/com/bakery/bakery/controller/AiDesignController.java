package com.bakery.bakery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.bakery.bakery.service.AiImageService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

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
        String imageData = aiImageService.generateCakeImage(prompt);

        if (imageData == null) {
            model.addAttribute("generatedImageUrl", null);
            model.addAttribute("userPrompt", prompt);
            return "ai-design";
        }

        // Якщо повернувся base64 — зберігаємо на диск
        if (imageData.startsWith("data:image")) {
            String savedUrl = saveBase64Image(imageData);
            model.addAttribute("generatedImageUrl", savedUrl != null ? savedUrl : imageData);
        } else {
            // Звичайний URL (dall-e-3)
            model.addAttribute("generatedImageUrl", imageData);
        }

        model.addAttribute("userPrompt", prompt);
        return "ai-design";
    }

    /**
     * Endpoint для збереження base64 зображення з wishlist/cart через JS
     * POST /api/ai-design/save  { "base64": "data:image/png;base64,..." }
     * Повертає { "url": "/uploads/ai-designs/xxx.png" }
     */
    @PostMapping("/api/ai-design/save")
    @ResponseBody
    public ResponseEntity<?> saveAiImage(@RequestBody Map<String, String> body) {
        String base64 = body.get("base64");
        if (base64 == null || !base64.startsWith("data:image")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid image data"));
        }
        String url = saveBase64Image(base64);
        if (url == null) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Save failed"));
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ── Зберігає base64 → uploads/ai-designs/uuid.png ────────────────────────
    private String saveBase64Image(String base64Data) {
        try {
            // Відрізаємо префікс "data:image/png;base64,"
            String base64 = base64Data.contains(",")
                    ? base64Data.substring(base64Data.indexOf(',') + 1)
                    : base64Data;

            byte[] bytes = Base64.getDecoder().decode(base64);

            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "ai-designs");
            Files.createDirectories(uploadDir);

            String filename = UUID.randomUUID().toString() + ".png";
            Path filePath = uploadDir.resolve(filename);

            try (OutputStream os = Files.newOutputStream(filePath)) {
                os.write(bytes);
            }

            return "/uploads/ai-designs/" + filename;

        } catch (IOException e) {
            System.err.println("Помилка збереження AI зображення: " + e.getMessage());
            return null;
        }
    }
}