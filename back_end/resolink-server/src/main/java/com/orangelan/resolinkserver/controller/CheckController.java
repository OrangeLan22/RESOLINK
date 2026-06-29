package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.PhysicalResource;
import com.orangelan.resolinkserver.entity.SpaceResource;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/check")
public class CheckController {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckController.class);
    
    private final AppointmentRepository appointmentRepository;
    private final SpaceResourceRepository spaceResourceRepository;
    private final PhysicalResourceRepository physicalResourceRepository;
    
    @Autowired
    public CheckController(AppointmentRepository appointmentRepository, 
                          SpaceResourceRepository spaceResourceRepository, 
                          PhysicalResourceRepository physicalResourceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.spaceResourceRepository = spaceResourceRepository;
        this.physicalResourceRepository = physicalResourceRepository;
    }
    
    @PostMapping("/pending-check")
    public ResponseEntity<Map<String, Object>> getPendingCheckResources() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            long currentTimeSec = System.currentTimeMillis() / 1000;
            long thresholdTime = currentTimeSec - 30 * 60;
            
            List<Appointment> allAppointments = appointmentRepository.findAll();
            List<Map<String, Object>> pendingCheckList = new ArrayList<>();
            
            for (Appointment appointment : allAppointments) {
                if (appointment.getStatus().equals("-1")) {
                    continue;
                }
                
                if (appointment.getCheck() != 0) {
                    continue;
                }
                
                try {
                    long endTimeSec = Long.parseLong(appointment.getEndTime());
                    if (endTimeSec + 30 * 60 >= currentTimeSec && endTimeSec <= currentTimeSec) {
                        Map<String, Object> appointmentMap = new LinkedHashMap<>();
                        appointmentMap.put("id", appointment.getId());
                        appointmentMap.put("empId", appointment.getEmpId());
                        appointmentMap.put("name", appointment.getName());
                        appointmentMap.put("resId", appointment.getResId());
                        appointmentMap.put("resName", appointment.getResName());
                        appointmentMap.put("type", appointment.getType());
                        
                        if (appointment.getAppointmentDate() != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            appointmentMap.put("appointmentDate", dateFormat.format(appointment.getAppointmentDate()));
                        }
                        
                        appointmentMap.put("startTime", appointment.getStartTime());
                        appointmentMap.put("endTime", appointment.getEndTime());
                        appointmentMap.put("status", appointment.getStatus());
                        appointmentMap.put("approval", appointment.getApproval());
                        appointmentMap.put("check", appointment.getCheck());
                        
                        String location = "";
                        if (appointment.getType() != null) {
                            if (appointment.getType().equals("space")) {
                                SpaceResource spaceResource = spaceResourceRepository.findById(appointment.getResId()).orElse(null);
                                if (spaceResource != null) {
                                    location = spaceResource.getLocation();
                                }
                            } else if (appointment.getType().equals("physical")) {
                                PhysicalResource physicalResource = physicalResourceRepository.findById(appointment.getResId()).orElse(null);
                                if (physicalResource != null) {
                                    location = physicalResource.getLocation();
                                }
                            }
                        }
                        appointmentMap.put("location", location);
                        
                        pendingCheckList.add(appointmentMap);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("解析预约时间失败：{}", e.getMessage());
                    continue;
                }
            }
            
            response.put("success", true);
            response.put("message", "获取待检查资源成功");
            response.put("data", pendingCheckList);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取待检查资源失败：{}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取待检查资源失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkStatus(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            if (request == null) {
                logger.error("检查状态失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!request.containsKey("data")) {
                logger.error("检查状态失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!request.containsKey("token")) {
                logger.error("检查状态失败：请求体缺少token字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("检查状态失败：data字段格式错误");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            if (!data.containsKey("boolean") || !data.containsKey("id")) {
                logger.error("检查状态失败：data中缺少boolean或id字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Object booleanObj = data.get("boolean");
            Object idObj = data.get("id");
            Object closeObj = data.get("close");
            
            if (!(booleanObj instanceof Boolean)) {
                logger.error("检查状态失败：boolean字段类型错误");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean boolValue = (Boolean) booleanObj;
            
            Long appointmentId;
            if (idObj instanceof Integer) {
                appointmentId = ((Integer) idObj).longValue();
            } else if (idObj instanceof Long) {
                appointmentId = (Long) idObj;
            } else {
                logger.error("检查状态失败：id字段类型错误");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
            if (!appointmentOptional.isPresent()) {
                logger.error("检查状态失败：预约记录不存在，ID：{}", appointmentId);
                response.put("success", false);
                response.put("message", "预约记录不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            if (boolValue) {
                appointment.setCheck(1);
                appointmentRepository.save(appointment);
                logger.info("检查通过，预约ID：{}", appointmentId);
            } else {
                appointment.setCheck(-1);
                
                boolean closeResource = false;
                if (closeObj instanceof Boolean) {
                    closeResource = (Boolean) closeObj;
                }
                
                if (closeResource) {
                    String type = appointment.getType();
                    Long resId = appointment.getResId();
                    
                    if ("space".equals(type)) {
                        Optional<SpaceResource> spaceResourceOptional = spaceResourceRepository.findById(resId);
                        if (spaceResourceOptional.isPresent()) {
                            SpaceResource spaceResource = spaceResourceOptional.get();
                            spaceResource.setStage(-1);
                            spaceResourceRepository.save(spaceResource);
                            logger.info("关闭空间资源成功，资源ID：{}", resId);
                        }
                    } else if ("physical".equals(type)) {
                        Optional<PhysicalResource> physicalResourceOptional = physicalResourceRepository.findById(resId);
                        if (physicalResourceOptional.isPresent()) {
                            PhysicalResource physicalResource = physicalResourceOptional.get();
                            physicalResource.setStage(-1);
                            physicalResourceRepository.save(physicalResource);
                            logger.info("关闭物理资源成功，资源ID：{}", resId);
                        }
                    }
                }
                
                appointmentRepository.save(appointment);
                logger.info("检查不通过，预约ID：{}，是否关闭资源：{}", appointmentId, closeResource);
            }
            
            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("boolean", boolValue);
            resultData.put("id", appointmentId);
            
            response.put("success", true);
            response.put("message", "检查状态更新成功");
            response.put("data", resultData);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查状态失败：{}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "检查状态失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}