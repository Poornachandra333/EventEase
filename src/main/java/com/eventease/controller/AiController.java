package com.eventease.controller;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventease.service.AiService;

@RestController
@ConditionalOnProperty(name = "spring.ai.openai.chat.enabled", havingValue = "true")
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    public record ChatRequest(List<Map<String, String>> messages) {}
    public record ChatResponse(String response) {}

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String responseContent = aiService.chat(request.messages());
        return ResponseEntity.ok(new ChatResponse(responseContent));
    }
}
