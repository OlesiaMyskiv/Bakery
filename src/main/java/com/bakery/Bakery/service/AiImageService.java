package com.bakery.Bakery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AiImageService {
    @Value("${google.gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public AiImageService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
    }

    public String generateCakeImage(String prompt) {
        // Логіка запиту до моделі imagen-3
        // 1. Формуємо JSON з промптом
        // 2. Відправляємо POST запит з вашим API ключем
        // 3. Отримуємо байтовий масив картинки
        // 4. Повертаємо посилання або Base64 рядок для HTML
        return "посилання_на_зображення";
    }

    
}