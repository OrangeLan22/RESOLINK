package com.orangelan.resolinkagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TokenValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    
    private final WebClient webClient;
    
    @Value("${back-end-server.url}")
    private String backendServerUrl;
    
    public TokenValidationService(WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<Boolean> validateToken(String token) {
        String validationUrl = backendServerUrl + "/api/token/validate";
        String requestBody = "{\"token\":\"" + token + "\"}";
        
        return webClient.post()
                .uri(validationUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    int statusCode = response.statusCode().value();
                    
                    if (statusCode == 200) {
                        return Mono.just(true);
                    } else {
                        return Mono.just(false);
                    }
                })
                .onErrorReturn(false);
    }
}