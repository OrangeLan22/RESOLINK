package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.entity.PhysicalResource;
import com.orangelan.resolinkagent.entity.SpaceResource;
import com.orangelan.resolinkagent.repository.AppointmentRepository;
import com.orangelan.resolinkagent.repository.PhysicalResourceRepository;
import com.orangelan.resolinkagent.repository.SpaceResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class AvailabilityCheckToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityCheckToolService.class);
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private PhysicalResourceRepository physicalResourceRepository;
    
    @Autowired
    private SpaceResourceRepository spaceResourceRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取预约可用性检查工具的定义
     * @return 工具定义
     */
    public Map<String, Object> getAvailabilityCheckToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "check_availability");
        function.put("description", "检查资源在指定时间段内的可用性");
        
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
        
        Map<String, Object> startTimeParam = new HashMap<>();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "预约开始时间，格式为YYYY-MM-DD-HH:MM:SS");
        properties.put("start_time", startTimeParam);
        
        Map<String, Object> endTimeParam = new HashMap<>();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "预约结束时间，格式为YYYY-MM-DD-HH:MM:SS");
        properties.put("end_time", endTimeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"id", "type", "start_time", "end_time"});
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }
    
    /**
     * 检查资源在指定时间段内的可用性
     * @param id 资源ID
     * @param type 资源类型
     * @param startTime 开始时间，格式为YYYY-MM-DD-HH:MM:SS
     * @param endTime 结束时间，格式为YYYY-MM-DD-HH:MM:SS
     * @return 检查结果
     */
    public String checkAvailability(Long id, String type, String startTime, String endTime) {
        try {
            logger.info("检查资源可用性，资源ID：{}，资源类型：{}，开始时间：{}，结束时间：{}", id, type, startTime, endTime);
            
            // 将时间字符串转换为时间戳
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            Date startDate = dateFormat.parse(startTime);
            Date endDate = dateFormat.parse(endTime);
            long startTimeStamp = startDate.getTime() / 1000; // 转换为秒级时间戳
            long endTimeStamp = endDate.getTime() / 1000;
            
            // 检查资源是否存在
            boolean resourceExists = false;
            boolean needsCheck = false; // 是否需要预留时间
            
            if ("physical".equalsIgnoreCase(type)) {
                Optional<PhysicalResource> resource = physicalResourceRepository.findById(id);
                if (resource.isPresent()) {
                    resourceExists = true;
                    needsCheck = resource.get().getCheckFlag() == 1;
                }
            } else if ("space".equalsIgnoreCase(type)) {
                Optional<SpaceResource> resource = spaceResourceRepository.findById(id);
                if (resource.isPresent()) {
                    resourceExists = true;
                    needsCheck = resource.get().getCheckFlag() == 1;
                }
            }
            
            if (!resourceExists) {
                return "资源不存在，请检查资源ID和类型是否正确";
            }
            
            // 检查时间冲突
            long conflictCount;
            if (needsCheck) {
                // 需要预留时间（前后各30分钟）
                long bufferedStartTime = startTimeStamp - 1800; // 减去30分钟
                long bufferedEndTime = endTimeStamp + 1800; // 加上30分钟
                conflictCount = appointmentRepository.countConflictingAppointmentsWithBuffer(id, type, bufferedStartTime, bufferedEndTime);
            } else {
                // 不需要预留时间
                conflictCount = appointmentRepository.countConflictingAppointments(id, type, startTimeStamp, endTimeStamp);
            }
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("resource_id", id);
            result.put("resource_type", type);
            result.put("start_time", startTime);
            result.put("end_time", endTime);
            result.put("needs_check", needsCheck);
            result.put("is_available", conflictCount == 0);
            result.put("conflict_count", conflictCount);
            
            // 添加字段说明
            Map<String, String> fieldDescriptions = new HashMap<>();
            fieldDescriptions.put("resource_id", "资源ID");
            fieldDescriptions.put("resource_type", "资源类型");
            fieldDescriptions.put("start_time", "预约开始时间");
            fieldDescriptions.put("end_time", "预约结束时间");
            fieldDescriptions.put("needs_check", "资源归还后是否需要检察员检查（1表示需要，0表示不需要）");
            fieldDescriptions.put("is_available", "资源在指定时间段内是否可用");
            fieldDescriptions.put("conflict_count", "与现有预约冲突的数量");
            result.put("field_descriptions", fieldDescriptions);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (ParseException e) {
            logger.error("时间格式解析失败", e);
            return "时间格式错误，请使用YYYY-MM-DD-HH:MM:SS格式";
        } catch (Exception e) {
            logger.error("检查资源可用性失败", e);
            return "检查资源可用性失败：" + e.getMessage();
        }
    }
    
    /**
     * 处理工具调用的函数映射
     * @return 函数名到实际执行函数的映射
     */
    public Map<String, Function<Map<String, Object>, String>> getToolFunctions() {
        Map<String, Function<Map<String, Object>, String>> functions = new HashMap<>();
        
        // 添加可用性检查函数
        functions.put("check_availability", params -> {
            Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
            String type = params.containsKey("type") ? (String) params.get("type") : null;
            String startTime = params.containsKey("start_time") ? (String) params.get("start_time") : null;
            String endTime = params.containsKey("end_time") ? (String) params.get("end_time") : null;
            return checkAvailability(id, type, startTime, endTime);
        });
        
        return functions;
    }
}