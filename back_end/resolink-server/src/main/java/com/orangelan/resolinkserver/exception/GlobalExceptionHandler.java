package com.orangelan.resolinkserver.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理类
 * 处理各种异常并返回友好的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理JSON解析异常
     * @param e JSON解析异常
     * @return 错误响应
     */
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseException(JsonParseException e) {
        logger.error("JSON解析错误：{}", e.getMessage());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", "JSON格式错误");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理JSON映射异常
     * @param e JSON映射异常
     * @return 错误响应
     */
    @ExceptionHandler(JsonMappingException.class)
    public ResponseEntity<Map<String, Object>> handleJsonMappingException(JsonMappingException e) {
        logger.error("JSON映射错误：{}", e.getMessage());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        
        // 根据异常类型提供更具体的错误信息
        if (e instanceof MismatchedInputException) {
            response.put("message", "JSON数据不完整或格式错误");
        } else if (e instanceof InvalidFormatException) {
            response.put("message", "JSON数据类型错误");
        } else {
            response.put("message", "JSON格式错误");
        }
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理通用异常
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        logger.error("系统错误：{}", e.getMessage(), e);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", "系统内部错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}