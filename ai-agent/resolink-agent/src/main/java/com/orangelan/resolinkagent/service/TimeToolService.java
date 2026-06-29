package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 时间工具服务，提供获取当前时间的功能
 */
@Service
public class TimeToolService {

    /**
     * 获取当前时间的工具定义
     * @return 工具定义的Map，符合DeepSeek API的工具调用格式
     */
    public Map<String, Object> getCurrentTimeToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "get_current_time");
        function.put("description", "获取当前的日期和时间，包括年、月、日、时、分、秒");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> formatProperty = new HashMap<>();
        formatProperty.put("type", "string");
        formatProperty.put("description", "时间格式，例如：'yyyy-MM-dd HH:mm:ss'、'yyyy年MM月dd日 HH:mm:ss'等。如果不提供，将使用默认格式");
        properties.put("format", formatProperty);
        
        Map<String, Object> timezoneProperty = new HashMap<>();
        timezoneProperty.put("type", "string");
        timezoneProperty.put("description", "时区，例如：'Asia/Shanghai'、'UTC'等。如果不提供，将使用系统默认时区");
        properties.put("timezone", timezoneProperty);
        
        parameters.put("properties", properties);
        
        // 设置可选参数
        String[] optionalParams = {"format", "timezone"};
        function.put("parameters", parameters);
        
        tool.put("function", function);
        
        return tool;
    }

    /**
     * 获取当前时间的实际实现
     * @param format 时间格式，可为空
     * @param timezone 时区，可为空
     * @return 格式化后的当前时间字符串
     */
    public String getCurrentTime(String format, String timezone) {
        try {
            // 使用默认格式如果没有提供
            if (format == null || format.trim().isEmpty()) {
                format = "yyyy-MM-dd HH:mm:ss";
            }
            
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            
            // 创建格式化器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            
            // 格式化时间
            String formattedTime = now.format(formatter);
            
            // 如果提供了时区，添加时区信息
            if (timezone != null && !timezone.trim().isEmpty()) {
                formattedTime += " (时区: " + timezone + ")";
            }
            
            return formattedTime;
        } catch (Exception e) {
            return "获取时间失败: " + e.getMessage();
        }
    }

    /**
     * 处理工具调用的函数映射
     * @return 函数名到实际执行函数的映射
     */
    public Map<String, Function<Map<String, Object>, String>> getToolFunctions() {
        Map<String, Function<Map<String, Object>, String>> functions = new HashMap<>();
        
        // 添加获取当前时间的函数
        functions.put("get_current_time", params -> {
            String format = params.containsKey("format") ? (String) params.get("format") : null;
            String timezone = params.containsKey("timezone") ? (String) params.get("timezone") : null;
            return getCurrentTime(format, timezone);
        });
        
        return functions;
    }
}