package com.goviconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public GeminiChatService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

   @SuppressWarnings("unchecked")
public Mono<String> getChatResponse(String prompt) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("contents", List.of(
            Map.of("role", "user",
                   "parts", List.of(Map.of("text", prompt)))));
    requestBody.put("generationConfig", Map.of("temperature", 0.7, "topP", 0.95, "topK", 40, "maxOutputTokens", 1024));

    // Correctly join the URL and the Key
    String fullUrl = apiUrl.trim() + apiKey.trim();

    return this.webClient.post()
            .uri(URI.create(fullUrl))
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                try {
                    // Extract the text response from Gemini API JSON structure
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        if (content != null) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                return (String) parts.get(0).get("text");
                            }
                        }
                    }
                    return "No response content found.";
                } catch (Exception e) {
                    return "Error parsing response: " + e.getMessage();
                }
            })
            .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class, ex -> {
                String errorBody = ex.getResponseBodyAsString();
                System.err.println("Gemini API Error [" + ex.getStatusCode() + "]: " + errorBody);
                
                // Diagnostic: List available models to find the correct name
                this.webClient.get()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(models -> System.out.println("DEBUG - Available Models for this Key: " + models));

                return Mono.just("Service temporarily unavailable. (Error " + ex.getStatusCode() + ")");
            });
}
}
