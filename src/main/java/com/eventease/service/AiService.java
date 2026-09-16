package com.eventease.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultSystem("You are the EventEase Assistant, a helpful AI integrated into an event booking platform. " +
                           "Your primary job is to help users discover events and check ticket availability. " +
                           "Use the provided tools to search for events and check ticket quantities. " +
                           "Always base your answers on the data returned by the tools. " +
                           "Do not invent or hallucinate events or ticket prices.")
            .defaultFunctions("searchEvents", "getTicketAvailability")
            .build();
    }

    public String chat(List<Map<String, String>> conversationHistory) {
        List<Message> messages = new ArrayList<>();
        
        for (Map<String, String> msg : conversationHistory) {
            String role = msg.get("role");
            String content = msg.get("content");
            if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            }
        }

        return chatClient.prompt(new Prompt(messages))
                .call()
                .content();
    }
}
