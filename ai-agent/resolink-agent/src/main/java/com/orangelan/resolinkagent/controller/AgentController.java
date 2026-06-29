package com.orangelan.resolinkagent.controller;

import com.orangelan.resolinkagent.dto.ChatRequest;
import com.orangelan.resolinkagent.dto.ChatResponse;
import com.orangelan.resolinkagent.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {
    
    private final ChatService chatService;
    
    @Autowired
    public AgentController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            // 确保请求体包含session_id字段，即使它为null
            ChatResponse response = chatService.processMessageWithSession(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse chatResponse = new ChatResponse(null, null, false);
            return ResponseEntity.status(500).body(chatResponse);
        }
    }
}