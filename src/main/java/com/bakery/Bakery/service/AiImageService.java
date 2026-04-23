package com.bakery.Bakery.service;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AiImageService {

    public String generateCakeDesign(String userPrompt) {
        try {
            // Формуємо магічний запит для ШІ
            String finalPrompt = "Professional studio photography of a realistic beautiful cake. Perfect lighting, bakery aesthetic. Details: " + userPrompt;

            // ШІ краще розуміє URL, коли там немає пробілів (кодуємо текст)
            String encodedPrompt = URLEncoder.encode(finalPrompt, StandardCharsets.UTF_8.toString());

            // Використовуємо БЕЗКОШТОВНИЙ сервіс Pollinations.ai (без API-ключів!)
            // Параметр nologo=true прибирає водяні знаки
            String imageUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=1024&height=1024&nologo=true";

            // Сервіс генерує картинку "на льоту" за цим посиланням
            return imageUrl;

        } catch (Exception e) {
            System.err.println("Помилка генерації зображення: " + e.getMessage());
            return null;
        }
    }
}