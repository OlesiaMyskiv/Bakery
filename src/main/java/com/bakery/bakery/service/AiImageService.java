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
              "model": "gpt-image-1",
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
            // gpt-image-1 повертає base64, dall-e-3 повертає url
            JsonNode dataNode = rootNode.path("data").get(0);
            if (dataNode.has("url")) {
                String imageUrl = dataNode.path("url").asText();
                System.out.println("Зображення отримано (url): " + imageUrl);
                return imageUrl;
            } else if (dataNode.has("b64_json")) {
                String b64 = dataNode.path("b64_json").asText();
                System.out.println("Зображення отримано (base64)");
                return "data:image/png;base64," + b64;
            }
            return null;

        } catch (Exception e) {
            System.err.println("Помилка: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}