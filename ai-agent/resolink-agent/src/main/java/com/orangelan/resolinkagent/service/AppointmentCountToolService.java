package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class AppointmentCountToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppointmentCountToolService.class);
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取预约次数查询工具的定义
     * @return 工具定义
     */
    public Map<String, Object> getAppointmentCountToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "query_appointment_count");
        function.put("description", "查询资源的预约次数");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> idParam = new HashMap<>();
        idParam.put("type", "integer");
        idParam.put("description", "资源ID");
        properties.put("id", idParam);
        
        Map<String, Object> typeParam = new HashMap<>();
        typeParam.put("type", "string");
        typeParam.put("description", "资源类型，\"space\"表示空间资源，\"physical\"表示物理资源");
        properties.put("type", typeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"id", "type"});
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }
    
    /**
     * 查询资源的预约次数
     * @param id 资源ID
     * @param type 资源类型
     * @return 查询结果
     */
    public String queryAppointmentCount(Long id, String type) {
        try {
            logger.info("查询预约次数，资源ID：{}，资源类型：{}", id, type);
            
            // 查询总预约次数
            Long totalCount = appointmentRepository.countByResIdAndType(id, type);
            
            // 查询有效预约次数（状态为正常的）
            Long validCount = appointmentRepository.countValidByResIdAndType(id, type);
            
            // 查询已取消预约次数（状态为已取消的）
            Long cancelledCount = appointmentRepository.countCancelledByResIdAndType(id, type);
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("resource_id", id);
            result.put("resource_type", type);
            result.put("total_appointment_count", totalCount);
            result.put("valid_appointment_count", validCount);
            result.put("cancelled_appointment_count", cancelledCount);
            
            // 添加字段说明
            Map<String, String> fieldDescriptions = new HashMap<>();
            fieldDescriptions.put("resource_id", "资源ID");
            fieldDescriptions.put("resource_type", "资源类型");
            fieldDescriptions.put("total_appointment_count", "总预约次数");
            fieldDescriptions.put("valid_appointment_count", "有效预约次数（状态为正常的预约）");
            fieldDescriptions.put("cancelled_appointment_count", "已取消预约次数（状态为已取消的预约）");
            result.put("field_descriptions", fieldDescriptions);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            logger.error("查询预约次数失败", e);
            return "查询预约次数失败：" + e.getMessage();
        }
    }
    
    /**
     * 处理工具调用的函数映射
     * @return 函数名到实际执行函数的映射
     */
    public Map<String, Function<Map<String, Object>, String>> getToolFunctions() {
        Map<String, Function<Map<String, Object>, String>> functions = new HashMap<>();
        
        // 添加预约次数查询函数（支持中英文函数名）
        Function<Map<String, Object>, String> appointmentCountFunction = params -> {
            // 支持多种参数名：id/resource_id, type/resource_type
            Long id = null;
            if (params.containsKey("id")) {
                id = ((Number) params.get("id")).longValue();
            } else if (params.containsKey("resource_id")) {
                id = ((Number) params.get("resource_id")).longValue();
            }
            
            String type = null;
            if (params.containsKey("type")) {
                type = (String) params.get("type");
            } else if (params.containsKey("resource_type")) {
                type = (String) params.get("resource_type");
            }
            
            return queryAppointmentCount(id, type);
        };
        
        functions.put("query_appointment_count", appointmentCountFunction);
        functions.put("query_reservation_count", appointmentCountFunction);
        
        return functions;
    }
}