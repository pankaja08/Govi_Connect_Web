package com.goviconnect.controller;

import com.goviconnect.service.GeminiChatService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatController {

    private final GeminiChatService geminiChatService;

    public ChatController(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    @PostMapping
    public Mono<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String userQuestion = request.get("question");
        
        // Add context for the agricultural application
        String contextualPrompt = "You are GoviConnect AI, a helpful agricultural expert assistant for Sri Lankan farmers. " +
                "Please answer the following question concisely and helpfuly: " + userQuestion;

        return geminiChatService.getChatResponse(contextualPrompt)
                .map(response -> Map.of("response", response));
    }
}
