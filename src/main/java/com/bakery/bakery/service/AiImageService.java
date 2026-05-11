package com.bakery.bakery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiImageService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateCakeImage(String userPrompt) {
        try {
            String fullPrompt = ("Photorealistic cake design, professional food photography: " + userPrompt)
                    .replace("\"", "\\\"");

            String requestBody = """
            {
              "model": "dall-e-3",
              "prompt": "%s",
              "n": 1,
              "size": "1024x1024"
            }
            """.formatted(fullPrompt);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/images/generations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Відповідь від OpenAI: " + response.body());

            JsonNode rootNode = objectMapper.readTree(response.body());

            if (rootNode.has("error")) {
                System.err.println("Помилка від OpenAI: " +
                        rootNode.get("error").path("message").asText());
                return null;
            }

            // URL зображення приходить прямо у відповіді
            String imageUrl = rootNode.path("data").get(0).path("url").asText();
            System.out.println("Зображення отримано: " + imageUrl);
            return imageUrl;

        } catch (Exception e) {
            System.err.println("Помилка: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}