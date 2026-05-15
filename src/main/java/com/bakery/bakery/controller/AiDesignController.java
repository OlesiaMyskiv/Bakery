package com.bakery.bakery.controller;

import com.bakery.bakery.model.AiDesign;
import com.bakery.bakery.model.User;
import com.bakery.bakery.repository.AiDesignRepository;
import com.bakery.bakery.repository.UserRepository;
import com.bakery.bakery.service.AiImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class AiDesignController {

    private final AiImageService aiImageService;
    private final AiDesignRepository aiDesignRepository;
    private final UserRepository userRepository;

    public AiDesignController(AiImageService aiImageService,
                              AiDesignRepository aiDesignRepository,
                              UserRepository userRepository) {
        this.aiImageService = aiImageService;
        this.aiDesignRepository = aiDesignRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/ai-design")
    public String showAiDesignPage(Model model) {
        // Спочатку показуємо файли з диску (навіть якщо БД порожня)
        List<String> galleryUrls = loadAiDesignUrlsFromDisk();
        model.addAttribute("galleryUrls", galleryUrls);
        return "ai-design";
    }

    @GetMapping("/ai-design/generate")
    public String redirectGenerate() {
        return "redirect:/ai-design";
    }

    @PostMapping("/ai-design/generate")
    public String generate(@RequestParam("prompt") String prompt,
                           Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        String imageData = aiImageService.generateCakeImage(prompt);

        String savedUrl = null;
        if (imageData != null) {
            if (imageData.startsWith("data:image")) {
                savedUrl = saveBase64Image(imageData);
            } else {
                savedUrl = imageData;
            }
        }

        // Зберігаємо в БД якщо користувач авторизований
        if (savedUrl != null && userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElse(null);
            AiDesign design = new AiDesign();
            design.setImageUrl(savedUrl);
            design.setPrompt(prompt);
            design.setUser(user);
            design.setPublic(false); // за замовчуванням не публічний
            aiDesignRepository.save(design);
        }

        List<String> galleryUrls = loadAiDesignUrlsFromDisk();
        model.addAttribute("galleryUrls", galleryUrls);
        model.addAttribute("generatedImageUrl", savedUrl);
        model.addAttribute("userPrompt", prompt);
        return "ai-design";
    }

    /**
     * Зробити дизайн публічним (після генерації користувач тисне "Поділитись з усіма")
     * POST /api/ai-design/publish  { "imageUrl": "/uploads/ai-designs/xxx.png", "prompt": "..." }
     */
    @PostMapping("/api/ai-design/publish")
    @ResponseBody
    public ResponseEntity<?> publishDesign(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        String imageUrl = body.get("imageUrl");
        String prompt = body.get("prompt");

        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No imageUrl"));
        }

        // Перевіряємо чи вже збережений, інакше — знаходимо і публікуємо
        AiDesign design = aiDesignRepository.findAll().stream()
                .filter(d -> imageUrl.equals(d.getImageUrl()))
                .findFirst()
                .orElse(null);

        if (design == null) {
            // Якщо не знайшли в БД — створюємо новий запис
            User user = null;
            if (userDetails != null) {
                user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            }
            design = new AiDesign();
            design.setImageUrl(imageUrl);
            design.setPrompt(prompt);
            design.setUser(user);
        }

        design.setPublic(true);
        aiDesignRepository.save(design);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * API: отримати всі публічні дизайни (для динамічного оновлення галереї)
     * GET /api/ai-design/public
     */
    @GetMapping("/api/ai-design/public")
    @ResponseBody
    public ResponseEntity<?> getPublicDesigns() {
        List<String> urls = loadAiDesignUrlsFromDisk();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            result.add(Map.<String, Object>of("id", i, "imageUrl", urls.get(i), "prompt", ""));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Зберегти base64 → файл на диск
     * POST /api/ai-design/save  { "base64": "data:image/png;base64,..." }
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

    // ── Зберігає base64 → uploads/ai-designs/uuid.png ───────────────────────
    private String saveBase64Image(String base64Data) {
        try {
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

    // ── Читає всі PNG з uploads/ai-designs і повертає URL список ────────────
    private List<String> loadAiDesignUrlsFromDisk() {
        List<String> urls = new java.util.ArrayList<>();
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "ai-designs");
            if (Files.exists(dir)) {
                Files.list(dir)
                        .filter(p -> p.toString().endsWith(".png") || p.toString().endsWith(".jpg"))
                        .sorted(java.util.Comparator.reverseOrder()) // найновіші першими
                        .forEach(p -> urls.add("/uploads/ai-designs/" + p.getFileName().toString()));
            }
        } catch (IOException e) {
            System.err.println("Помилка читання ai-designs: " + e.getMessage());
        }
        return urls;
    }
}