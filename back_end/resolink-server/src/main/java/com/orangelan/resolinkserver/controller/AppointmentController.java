package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.Department;
import com.orangelan.resolinkserver.entity.Status;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.entity.SpaceResource;
import com.orangelan.resolinkserver.entity.PhysicalResource;
import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.Authority;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.repository.StatusRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import com.orangelan.resolinkserver.repository.AdminAccountRepository;
import com.orangelan.resolinkserver.repository.AuthorityRepository;
import com.orangelan.resolinkserver.service.LarkMessageService;
import com.orangelan.resolinkserver.service.AppointmentNotificationService;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 预约管理控制器
 * 用于处理预约相关的API请求
 */
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    
    private final AppointmentRepository appointmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserInfoRepository userInfoRepository;
    private final SpaceResourceRepository spaceResourceRepository;
    private final PhysicalResourceRepository physicalResourceRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final AuthorityRepository authorityRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final LarkMessageService larkMessageService;
    private final AppointmentNotificationService appointmentNotificationService;
    
    public AppointmentController(AppointmentRepository appointmentRepository, 
                               UserAccountRepository userAccountRepository, UserInfoRepository userInfoRepository, 
                               SpaceResourceRepository spaceResourceRepository, PhysicalResourceRepository physicalResourceRepository, 
                               AdminAccountRepository adminAccountRepository, AuthorityRepository authorityRepository, 
                               JwtTokenUtil jwtTokenUtil, LarkMessageService larkMessageService, 
                               AppointmentNotificationService appointmentNotificationService) {
        this.appointmentRepository = appointmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.userInfoRepository = userInfoRepository;
        this.spaceResourceRepository = spaceResourceRepository;
        this.physicalResourceRepository = physicalResourceRepository;
        this.adminAccountRepository = adminAccountRepository;
        this.authorityRepository = authorityRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.larkMessageService = larkMessageService;
        this.appointmentNotificationService = appointmentNotificationService;
    }
    
    /**
     * 创建预约API
     * @param request 请求体，包含创建预约所需的数据
     * @return 创建结果
     */
    @PostMapping("/create-appointment")
    public ResponseEntity<Map<String, Object>> createAppointment(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("创建预约失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("创建预约失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("创建预约失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("emp_id")) {
                logger.error("创建预约失败：data字段缺少emp_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("name")) {
                logger.error("创建预约失败：data字段缺少name");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("res_id")) {
                logger.error("创建预约失败：data字段缺少res_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("res_name")) {
                logger.error("创建预约失败：data字段缺少res_name");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("appointment_date")) {
                logger.error("创建预约失败：data字段缺少appointment_date");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("start_time")) {
                logger.error("创建预约失败：data字段缺少start_time");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("end_time")) {
                logger.error("创建预约失败：data字段缺少end_time");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("type")) {
                logger.error("创建预约失败：data字段缺少type");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证必要字段的值
            String empId = data.get("emp_id") instanceof String ? (String) data.get("emp_id") : null;
            if (empId == null || empId.trim().isEmpty()) {
                logger.error("创建预约失败：emp_id不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            String name = data.get("name") instanceof String ? (String) data.get("name") : null;
            if (name == null || name.trim().isEmpty()) {
                logger.error("创建预约失败：name不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            

            
            Long resId = null;
            Object resIdObj = data.get("res_id");
            if (resIdObj instanceof Number) {
                resId = ((Number) resIdObj).longValue();
            } else if (resIdObj instanceof String) {
                try {
                    resId = Long.parseLong((String) resIdObj);
                } catch (NumberFormatException e) {
                    logger.error("创建预约失败：res_id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (resId == null) {
                logger.error("创建预约失败：res_id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            String resName = data.get("res_name") instanceof String ? (String) data.get("res_name") : null;
            if (resName == null || resName.trim().isEmpty()) {
                logger.error("创建预约失败：res_name不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Date appointmentDate = null;
            Object appointmentDateObj = data.get("appointment_date");
            if (appointmentDateObj instanceof Date) {
                appointmentDate = (Date) appointmentDateObj;
            } else if (appointmentDateObj instanceof String) {
                try {
                    // 使用SimpleDateFormat解析日期字符串，格式为yyyy-MM-dd
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    appointmentDate = dateFormat.parse((String) appointmentDateObj);
                } catch (Exception e) {
                    logger.error("创建预约失败：appointment_date格式错误");
                    response.put("success", false);
                    response.put("message", "日期格式错误，请使用yyyy-MM-dd格式");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (appointmentDate == null) {
                logger.error("创建预约失败：appointment_date不是有效的日期");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            String startTime = data.get("start_time") instanceof String ? (String) data.get("start_time") : null;
            if (startTime == null || startTime.trim().isEmpty()) {
                logger.error("创建预约失败：start_time不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            String endTime = data.get("end_time") instanceof String ? (String) data.get("end_time") : null;
            if (endTime == null || endTime.trim().isEmpty()) {
                logger.error("创建预约失败：end_time不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            String type = data.get("type") instanceof String ? (String) data.get("type") : null;
            if (type == null || type.trim().isEmpty()) {
                logger.error("创建预约失败：type不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            

            
            // 验证预约时间不能早于当前时间
            long currentTimeSec = System.currentTimeMillis() / 1000;
            long startSec = Long.parseLong(startTime);
            long endSec = Long.parseLong(endTime);
            
            if (startSec < currentTimeSec) {
                logger.error("创建预约失败：预约开始时间不能早于当前时间");
                response.put("success", false);
                response.put("message", "预约开始时间不能早于当前时间");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (endSec < currentTimeSec) {
                logger.error("创建预约失败：预约结束时间不能早于当前时间");
                response.put("success", false);
                response.put("message", "预约结束时间不能早于当前时间");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证预约时间段不能与已有预约或检查时间冲突
            List<Appointment> existingAppointments = appointmentRepository.findAll();
            
            // 检查资源是否需要检查
            boolean needCheck = false;
            if (type.equals("space")) {
                SpaceResource spaceResource = spaceResourceRepository.findById(resId).orElse(null);
                if (spaceResource != null && spaceResource.getCheckFlag() == 1) {
                    needCheck = true;
                }
            } else if (type.equals("physical")) {
                PhysicalResource physicalResource = physicalResourceRepository.findById(resId).orElse(null);
                if (physicalResource != null && physicalResource.getCheckFlag() == 1) {
                    needCheck = true;
                }
            }
            
            for (Appointment existingAppointment : existingAppointments) {
                if (existingAppointment.getType().equals(type) && 
                    existingAppointment.getResId().equals(resId) && 
                    !existingAppointment.getStatus().equals("-1")) {
                    
                    try {
                        long existingStartSec = Long.parseLong(existingAppointment.getStartTime());
                        long existingEndSec = Long.parseLong(existingAppointment.getEndTime());
                        
                        // 检查预约时间是否冲突
                        if (!(endSec <= existingStartSec || startSec >= existingEndSec)) {
                            logger.error("创建预约失败：预约时间段与已有预约冲突");
                            response.put("success", false);
                            response.put("message", "该时间段已被预约");
                            return ResponseEntity.badRequest().body(response);
                        }
                        
                        // 如果需要检查，检查是否与检查时间冲突
                        if (needCheck) {
                            // 预约开始前的检查时间
                            long existingCheckStartBefore = existingStartSec - 1800; // 30分钟
                            long existingCheckEndBefore = existingStartSec;
                            
                            // 检查是否与预约开始前的检查时间冲突
                            if (!(endSec <= existingCheckStartBefore || startSec >= existingCheckEndBefore)) {
                                logger.error("创建预约失败：预约时间段与已有预约的检查时间冲突");
                                response.put("success", false);
                                response.put("message", "该时间段与已有预约的检查时间冲突");
                                return ResponseEntity.badRequest().body(response);
                            }
                            
                            // 预约结束后的检查时间
                            long existingCheckStartAfter = existingEndSec;
                            long existingCheckEndAfter = existingEndSec + 1800; // 30分钟
                            
                            // 检查是否与预约结束后的检查时间冲突
                            if (!(endSec <= existingCheckStartAfter || startSec >= existingCheckEndAfter)) {
                                logger.error("创建预约失败：预约时间段与已有预约的检查时间冲突");
                                response.put("success", false);
                                response.put("message", "该时间段与已有预约的检查时间冲突");
                                return ResponseEntity.badRequest().body(response);
                            }
                        }
                    } catch (Exception e) {
                        continue; // 解析失败，跳过该记录
                    }
                }
            }
            
            // 创建Appointment实体
            Appointment appointment = new Appointment();
            appointment.setEmpId(empId);
            appointment.setName(name);
            appointment.setResId(resId);
            appointment.setResName(resName);
            appointment.setType(type);
            appointment.setAppointmentDate(appointmentDate);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setStatus("0");
            appointment.setApproval(0);
            
            // 保存到数据库，获取保存后的实体（包含生成的ID）
            appointment = appointmentRepository.save(appointment);
            
            logger.info("预约创建成功：员工工号：{}，资源：{}，预约ID：{}", empId, resName, appointment.getId());
            
            // 手动添加预约通知任务
            appointmentNotificationService.addAppointmentNotification(appointment);
            
            // 尝试发送飞书消息通知用户
            try {
                // 获取用户信息，查找飞书用户ID
                UserInfo userInfo = userInfoRepository.findByEmpId(empId);
                if (userInfo != null && userInfo.getFeishuUserId() != null) {
                    // 获取资源位置
                    String location = "";
                    if (type.equals("space")) {
                        SpaceResource spaceResource = spaceResourceRepository.findById(resId).orElse(null);
                        if (spaceResource != null) {
                            location = spaceResource.getLocation();
                        }
                    } else if (type.equals("physical")) {
                        PhysicalResource physicalResource = physicalResourceRepository.findById(resId).orElse(null);
                        if (physicalResource != null) {
                            location = physicalResource.getLocation();
                        }
                    }
                    
                    // 格式化时间
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    String dateStr = dateFormat.format(appointmentDate);
                    Date startTimeDate = new Date(Long.parseLong(startTime) * 1000);
                    Date endTimeDate = new Date(Long.parseLong(endTime) * 1000);
                    String startTimeStr = timeFormat.format(startTimeDate);
                    String endTimeStr = timeFormat.format(endTimeDate);
                    String timeRange = dateStr + " " + startTimeStr + " - " + endTimeStr;
                    
                    // 构建预约成功卡片消息（使用模板卡片ID）
                    String cardContent = "{\n" +
                        "  \"type\": \"template\",\n" +
                        "  \"data\": {\n" +
                        "    \"template_id\": \"AAqKDExTBpCLG\",\n" +
                        "    \"template_variable\": {\n" +
                        "      \"RESO_NAME\": \"" + resName + "\",\n" +
                        "      \"LOCATION\": \"" + location + "\",\n" +
                        "      \"START_TIME\": \"" + dateStr + " " + startTimeStr + "\",\n" +
                        "      \"END_TIME\": \"" + dateStr + " " + endTimeStr + "\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                    
                    // 发送卡片消息
                    boolean sendResult = larkMessageService.sendCardMessage(userInfo.getFeishuUserId(), cardContent, "open_id");
                    logger.info("发送预约成功通知结果：{}", sendResult);
                }
            } catch (Exception e) {
                logger.error("发送预约成功通知失败：{}", e.getMessage());
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "预约创建成功");
            response.put("data", appointment);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("创建预约失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("创建预约失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "预约创建失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("创建预约失败：系统错误", e);
            response.put("success", false);
            response.put("message", "预约创建失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    
    /**
     * 获取可预约的资源API
     * @param request 请求体，包含data和token参数
     * @return 可预约的资源列表
     */
    @PostMapping("/available-resources")
    public ResponseEntity<Map<String, Object>> getAvailableResources(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("获取可预约资源失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("获取可预约资源失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含token字段
            if (!request.containsKey("token")) {
                logger.error("获取可预约资源失败：请求体缺少token字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("获取可预约资源失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("type")) {
                logger.error("获取可预约资源失败：data字段缺少type");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("islocal")) {
                logger.error("获取可预约资源失败：data字段缺少islocal");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("iseffective")) {
                logger.error("获取可预约资源失败：data字段缺少iseffective");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证type字段
            String type = data.get("type") instanceof String ? (String) data.get("type") : null;
            if (type == null || (!type.equals("space") && !type.equals("physical"))) {
                logger.error("获取可预约资源失败：type字段无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证islocal字段
            Boolean islocal = null;
            Object islocalObj = data.get("islocal");
            if (islocalObj instanceof Boolean) {
                islocal = (Boolean) islocalObj;
            } else if (islocalObj instanceof String) {
                try {
                    islocal = Boolean.parseBoolean((String) islocalObj);
                } catch (Exception e) {
                    logger.error("获取可预约资源失败：islocal不是有效的布尔值");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (islocal == null) {
                logger.error("获取可预约资源失败：islocal参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证iseffective字段
            Boolean iseffective = null;
            Object iseffectiveObj = data.get("iseffective");
            if (iseffectiveObj instanceof Boolean) {
                iseffective = (Boolean) iseffectiveObj;
            } else if (iseffectiveObj instanceof String) {
                try {
                    iseffective = Boolean.parseBoolean((String) iseffectiveObj);
                } catch (Exception e) {
                    logger.error("获取可预约资源失败：iseffective不是有效的布尔值");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (iseffective == null) {
                logger.error("获取可预约资源失败：iseffective参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取start_time字段（非必填）
            Long startTimeStamp = null;
            Object startTimeObj = data.get("start_time");
            if (startTimeObj instanceof Number) {
                startTimeStamp = ((Number) startTimeObj).longValue() * 1000;
            } else if (startTimeObj instanceof String) {
                try {
                    startTimeStamp = Long.parseLong((String) startTimeObj) * 1000;
                } catch (NumberFormatException e) {
                    logger.error("获取可预约资源失败：start_time不是有效的时间戳");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 获取tag字段（非必填）
            String tag = data.get("tag") instanceof String ? (String) data.get("tag") : null;
            
            // 获取token字段
            String token = request.get("token") instanceof String ? (String) request.get("token") : null;
            if (token == null || token.trim().isEmpty()) {
                logger.error("获取可预约资源失败：token字段不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取Token（如果有Bearer前缀则移除）
            String tokenValue = token.trim();
            if (tokenValue.startsWith("Bearer ")) {
                tokenValue = tokenValue.substring(7).trim();
            }
            
            // 解析Token中的sub字段
            String sub = jwtTokenUtil.getUsernameFromToken(tokenValue);
            if (sub == null) {
                logger.error("获取可预约资源失败：Token解析失败");
                response.put("success", false);
                response.put("message", "Token无效");
                return ResponseEntity.badRequest().body(response);
            }
            

            
            // 根据type查询对应的资源
            List<Map<String, Object>> availableResources = new ArrayList<>();
            
            if (type.equals("space")) {
                // 查询空间资源
                List<SpaceResource> spaceResources = spaceResourceRepository.findAll();
                
                for (SpaceResource spaceResource : spaceResources) {
                    // 检查资源是否有效
                    if (iseffective && spaceResource.getStage() == -1) {
                        continue;
                    }
                    
                    // 根据tag字段过滤资源
                    if (tag != null && !tag.trim().isEmpty()) {
                        String resourceTag = spaceResource.getTag();
                        if (resourceTag == null || !resourceTag.contains(tag.trim())) {
                            continue;
                        }
                    }
                    

                    
                    // 检查资源是否可预约
                    boolean isAvailable = true;
                    int maxAvailableMinutes = 0;
                    
                    if (startTimeStamp != null) {
                        // 查询appointment表中该资源的预约记录
                        List<Appointment> appointments = appointmentRepository.findAll();
                        List<Map<String, String>> appointmentTimes = new ArrayList<>();
                        
                        for (Appointment appointment : appointments) {
                            if (appointment.getType().equals("space") && appointment.getResId().equals(spaceResource.getId()) && !appointment.getStatus().equals("-1")) {
                                Map<String, String> timeSlot = new LinkedHashMap<>();
                                timeSlot.put("start", appointment.getStartTime());
                                timeSlot.put("end", appointment.getEndTime());
                                appointmentTimes.add(timeSlot);
                            }
                        }
                        
                        // 计算可预约时间
                        maxAvailableMinutes = calculateMaxAvailableTime(startTimeStamp, appointmentTimes, spaceResource.getCheckFlag() == 1);
                        if (maxAvailableMinutes <= 0) {
                            isAvailable = false;
                        }
                    }
                    
                    if (isAvailable) {
                        Map<String, Object> resourceData = new LinkedHashMap<>();
                        resourceData.put("id", spaceResource.getId());
                        resourceData.put("name", spaceResource.getSpaceName());
                        resourceData.put("location", spaceResource.getLocation());
                        resourceData.put("type", spaceResource.getType());
                        resourceData.put("capacity", spaceResource.getCapacity());
                        resourceData.put("public", spaceResource.getPublicFlag());

                        resourceData.put("check", spaceResource.getCheckFlag());
                        resourceData.put("note", spaceResource.getNote());
                        resourceData.put("stage", spaceResource.getStage());
                        if (startTimeStamp != null) {
                            resourceData.put("max_available_minutes", maxAvailableMinutes);
                        }
                        availableResources.add(resourceData);
                    }
                }
            } else if (type.equals("physical")) {
                // 查询实物资源
                List<PhysicalResource> physicalResources = physicalResourceRepository.findAll();
                
                for (PhysicalResource physicalResource : physicalResources) {
                    // 检查资源是否有效
                    if (iseffective && physicalResource.getStage() == -1) {
                        continue;
                    }
                    
                    // 根据tag字段过滤资源
                    if (tag != null && !tag.trim().isEmpty()) {
                        String resourceTag = physicalResource.getTag();
                        if (resourceTag == null || !resourceTag.contains(tag.trim())) {
                            continue;
                        }
                    }
                    

                    
                    // 检查资源是否可预约
                    boolean isAvailable = true;
                    int maxAvailableMinutes = 0;
                    
                    if (startTimeStamp != null) {
                        // 查询appointment表中该资源的预约记录
                        List<Appointment> appointments = appointmentRepository.findAll();
                        List<Map<String, String>> appointmentTimes = new ArrayList<>();
                        
                        for (Appointment appointment : appointments) {
                            if (appointment.getType().equals("physical") && appointment.getResId().equals(physicalResource.getId()) && !appointment.getStatus().equals("-1")) {
                                Map<String, String> timeSlot = new LinkedHashMap<>();
                                timeSlot.put("start", appointment.getStartTime());
                                timeSlot.put("end", appointment.getEndTime());
                                appointmentTimes.add(timeSlot);
                            }
                        }
                        
                        // 计算可预约时间
                        maxAvailableMinutes = calculateMaxAvailableTime(startTimeStamp, appointmentTimes, physicalResource.getCheckFlag() == 1);
                        if (maxAvailableMinutes <= 0) {
                            isAvailable = false;
                        }
                    }
                    
                    if (isAvailable) {
                        Map<String, Object> resourceData = new LinkedHashMap<>();
                        resourceData.put("id", physicalResource.getId());
                        resourceData.put("name", physicalResource.getEquipmentName());
                        resourceData.put("location", physicalResource.getLocation());
                        resourceData.put("type", physicalResource.getType());
                        resourceData.put("public", physicalResource.getPublicFlag());

                        resourceData.put("check", physicalResource.getCheckFlag());
                        resourceData.put("note", physicalResource.getNote());
                        resourceData.put("stage", physicalResource.getStage());
                        if (startTimeStamp != null) {
                            resourceData.put("max_available_minutes", maxAvailableMinutes);
                        }
                        availableResources.add(resourceData);
                    }
                }
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "获取可预约资源成功");
            response.put("data", availableResources);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("获取可预约资源失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取可预约资源失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取可预约资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取可预约资源失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 取消预约API
     * @param request 请求体，包含取消预约所需的数据
     * @return 取消结果
     */
    @PostMapping("/cancel-appointment")
    public ResponseEntity<Map<String, Object>> cancelAppointment(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("取消预约失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("取消预约失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含token字段
            if (!request.containsKey("token")) {
                logger.error("取消预约失败：请求体缺少token字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("取消预约失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("id")) {
                logger.error("取消预约失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段
            Long appointmentId = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                appointmentId = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    appointmentId = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.error("取消预约失败：id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (appointmentId == null) {
                logger.error("取消预约失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取token字段
            String token = request.get("token") instanceof String ? (String) request.get("token") : null;
            if (token == null || token.trim().isEmpty()) {
                logger.error("取消预约失败：token字段不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取Token（如果有Bearer前缀则移除）
            String tokenValue = token.trim();
            if (tokenValue.startsWith("Bearer ")) {
                tokenValue = tokenValue.substring(7).trim();
            }
            
            // 解析Token中的sub字段
            String sub = jwtTokenUtil.getUsernameFromToken(tokenValue);
            if (sub == null) {
                logger.error("取消预约失败：Token解析失败");
                response.put("success", false);
                response.put("message", "Token无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 查询预约记录
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
            if (!appointmentOptional.isPresent()) {
                logger.error("取消预约失败：预约记录不存在");
                response.put("success", false);
                response.put("message", "预约记录不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // 检查预约状态
            if (appointment.getStatus().equals("-1")) {
                logger.error("取消预约失败：预约已取消");
                response.put("success", false);
                response.put("message", "预约已取消");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查是否为管理员
            boolean isAdmin = false;
            List<AdminAccount> adminAccounts = adminAccountRepository.findAll();
            for (AdminAccount adminAccount : adminAccounts) {
                if (adminAccount.getAdacc().equals(sub)) {
                    isAdmin = true;
                    break;
                }
            }
            
            // 检查权限：管理员可以取消任何预约，普通用户只能取消自己的预约
            if (!isAdmin) {
                // 普通用户需要根据sub字段获取对应的emp_id
                UserAccount userAccount = userAccountRepository.findByUseracc(sub);
                if (userAccount == null || !appointment.getEmpId().equals(userAccount.getEmpId())) {
                    logger.error("取消预约失败：无权限取消他人预约");
                    response.put("success", false);
                    response.put("message", "无权限取消他人预约");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 检查时间：现在时间戳是否在开始和结束时间内不允许取消
            long currentTimeSec = System.currentTimeMillis() / 1000;
            long startTimeSec = Long.parseLong(appointment.getStartTime());
            long endTimeSec = Long.parseLong(appointment.getEndTime());
            
            if (currentTimeSec >= startTimeSec && currentTimeSec <= endTimeSec) {
                logger.error("取消预约失败：资源使用中不可取消预约");
                response.put("success", false);
                response.put("message", "资源使用中不可取消预约");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查时间：已经结束的不可取消
            if (currentTimeSec > endTimeSec) {
                logger.error("取消预约失败：预约已结束，不可取消");
                response.put("success", false);
                response.put("message", "预约已结束，不可取消");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新预约状态为-1
            appointment.setStatus("-1");
            appointmentRepository.save(appointment);
            
            // 从消息队列中移除该记录
            appointmentNotificationService.removeAppointmentNotification(appointmentId);
            
            // 发送取消预约卡片消息
            try {
                // 获取预约人的员工ID
                String empId = appointment.getEmpId();
                // 根据员工ID查询用户信息，获取飞书用户ID
                UserInfo userInfo = userInfoRepository.findByEmpId(empId);
                if (userInfo != null && userInfo.getFeishuUserId() != null && !userInfo.getFeishuUserId().isEmpty()) {
                    // 获取资源名称
                    String resName = appointment.getResName();
                    // 获取资源位置
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
                    // 获取预约时间
                    Date startTimeDate = new Date(startTimeSec * 1000);
                    Date endTimeDate = new Date(endTimeSec * 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    String dateStr = dateFormat.format(startTimeDate);
                    String startTimeStr = timeFormat.format(startTimeDate);
                    String endTimeStr = timeFormat.format(endTimeDate);
                    
                    // 构建取消预约卡片消息（使用模板卡片ID）
                    String cardContent = "{\n" +
                        "  \"type\": \"template\",\n" +
                        "  \"data\": {\n" +
                        "    \"template_id\": \"AAqKDndJQ0IDW\",\n" +
                        "    \"template_variable\": {\n" +
                        "      \"RESO_NAME\": \"" + resName + "\",\n" +
                        "      \"LOCATION\": \"" + location + "\",\n" +
                        "      \"START_TIME\": \"" + dateStr + " " + startTimeStr + "\",\n" +
                        "      \"END_TIME\": \"" + dateStr + " " + endTimeStr + "\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                    
                    // 发送卡片消息
                    boolean sendResult = larkMessageService.sendCardMessage(userInfo.getFeishuUserId(), cardContent, "open_id");
                    logger.info("发送取消预约通知结果：{}", sendResult);
                }
            } catch (Exception e) {
                logger.error("发送取消预约通知失败：{}", e.getMessage());
            }
            
            logger.info("预约取消成功：预约ID：{}，员工工号：{}，资源：{}", appointmentId, appointment.getEmpId(), appointment.getResName());
            
            // 构造响应
            response.put("success", true);
            response.put("message", "预约取消成功");
            response.put("data", appointment);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("取消预约失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("取消预约失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "预约取消失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("取消预约失败：系统错误", e);
            response.put("success", false);
            response.put("message", "预约取消失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 提前释放预约API
     * @param request 请求体，包含提前释放预约所需的数据
     * @return 提前释放结果
     */
    @PostMapping("/release-appointment")
    public ResponseEntity<Map<String, Object>> releaseAppointment(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("提前释放预约失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("提前释放预约失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含token字段
            if (!request.containsKey("token")) {
                logger.error("提前释放预约失败：请求体缺少token字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("提前释放预约失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("id")) {
                logger.error("提前释放预约失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段
            Long appointmentId = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                appointmentId = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    appointmentId = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.error("提前释放预约失败：id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (appointmentId == null) {
                logger.error("提前释放预约失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取token字段
            String token = request.get("token") instanceof String ? (String) request.get("token") : null;
            if (token == null || token.trim().isEmpty()) {
                logger.error("提前释放预约失败：token字段不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取Token（如果有Bearer前缀则移除）
            String tokenValue = token.trim();
            if (tokenValue.startsWith("Bearer ")) {
                tokenValue = tokenValue.substring(7).trim();
            }
            
            // 解析Token中的sub字段
            String sub = jwtTokenUtil.getUsernameFromToken(tokenValue);
            if (sub == null) {
                logger.error("提前释放预约失败：Token解析失败");
                response.put("success", false);
                response.put("message", "Token无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 查询预约记录
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
            if (!appointmentOptional.isPresent()) {
                logger.error("提前释放预约失败：预约记录不存在");
                response.put("success", false);
                response.put("message", "预约记录不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // 检查预约状态
            if (appointment.getStatus().equals("-1")) {
                logger.error("提前释放预约失败：预约已取消");
                response.put("success", false);
                response.put("message", "预约已取消");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查是否为管理员
            boolean isAdmin = false;
            List<AdminAccount> adminAccounts = adminAccountRepository.findAll();
            for (AdminAccount adminAccount : adminAccounts) {
                if (adminAccount.getAdacc().equals(sub)) {
                    isAdmin = true;
                    break;
                }
            }
            
            // 检查权限：管理员可以释放任何预约，普通用户只能释放自己的预约
            if (!isAdmin && !appointment.getEmpId().equals(sub)) {
                logger.error("提前释放预约失败：无权限释放他人预约");
                response.put("success", false);
                response.put("message", "无权限释放他人预约");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查预约开始时间：只有在预约开始后才能提前释放
            long currentTimeSec = System.currentTimeMillis() / 1000;
            long startTimeSec = Long.parseLong(appointment.getStartTime());
            
            if (currentTimeSec < startTimeSec) {
                logger.error("提前释放预约失败：预约尚未开始，无法提前释放");
                response.put("success", false);
                response.put("message", "预约尚未开始，无法提前释放");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查预约结束时间：如果已经结束，无法提前释放
            long endTimeSec = Long.parseLong(appointment.getEndTime());
            if (currentTimeSec >= endTimeSec) {
                logger.error("提前释放预约失败：预约已结束，无需提前释放");
                response.put("success", false);
                response.put("message", "预约已结束，无需提前释放");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新预约结束时间为当前时间
            String currentTimeStr = String.valueOf(currentTimeSec);
            appointment.setEndTime(currentTimeStr);
            appointmentRepository.save(appointment);
            
            // 从消息队列中移除该记录
            appointmentNotificationService.removeAppointmentNotification(appointmentId);
            
            // 立即发送通知给资源检查员
            try {
                // 获取资源名称
                String resName = appointment.getResName();
                // 获取资源位置
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
                // 获取预约时间
                Date startTimeDate = new Date(startTimeSec * 1000);
                Date endTimeDate = new Date(currentTimeSec * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                String dateStr = dateFormat.format(startTimeDate);
                String startTimeStr = timeFormat.format(startTimeDate);
                String endTimeStr = timeFormat.format(endTimeDate);
                
                // 构建待检查卡片消息（使用模板卡片ID）
                String cardContent = "{\n" +
                    "  \"type\": \"template\",\n" +
                    "  \"data\": {\n" +
                    "    \"template_id\": \"AAqKSYg0BsbPQ\",\n" +
                    "    \"template_variable\": {\n" +
                    "      \"RESO_NAME\": \"" + resName + "\",\n" +
                    "      \"ID\": \"" + appointment.getResId() + "\",\n" +
                    "      \"LOCATION\": \"" + location + "\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
                
                // 获取资源检查员列表
                List<UserInfo> inspectors = getResourceInspectors();
                
                // 发送卡片消息给所有资源检查员
                for (UserInfo inspector : inspectors) {
                    if (inspector != null && inspector.getFeishuUserId() != null && !inspector.getFeishuUserId().isEmpty()) {
                        boolean sendResult = larkMessageService.sendCardMessage(inspector.getFeishuUserId(), cardContent, "open_id");
                        logger.info("发送提前释放通知结果给检查员 {}：{}", inspector.getName(), sendResult);
                    }
                }
            } catch (Exception e) {
                logger.error("发送提前释放通知失败：{}", e.getMessage());
            }
            
            logger.info("预约提前释放成功：预约ID：{}，员工工号：{}，资源：{}", appointmentId, appointment.getEmpId(), appointment.getResName());
            
            // 构造响应
            response.put("success", true);
            response.put("message", "预约提前释放成功");
            response.put("data", appointment);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("提前释放预约失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("提前释放预约失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "预约提前释放失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("提前释放预约失败：系统错误", e);
            response.put("success", false);
            response.put("message", "预约提前释放失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取资源检查员列表
     * @return 资源检查员列表
     */
    private List<UserInfo> getResourceInspectors() {
        List<UserInfo> inspectors = new ArrayList<>();
        try {
            // 从权限表中获取check字段为1的记录
            List<Authority> authorities = authorityRepository.findAll();
            List<Long> checkAuthIds = new ArrayList<>();
            for (Authority authority : authorities) {
                if (authority.getCheck() == 1) {
                    checkAuthIds.add(authority.getId());
                }
            }
            
            // 从user_info表中获取auth_id与这些权限ID匹配的用户
            if (!checkAuthIds.isEmpty()) {
                List<UserInfo> allUsers = userInfoRepository.findAll();
                for (UserInfo user : allUsers) {
                    if (checkAuthIds.contains(user.getAuthId()) && user.getFeishuUserId() != null && !user.getFeishuUserId().isEmpty()) {
                        inspectors.add(user);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取资源检查员失败：{}", e.getMessage());
        }
        return inspectors;
    }
    
    /**
     * 计算最大可预约时间（分钟）
     * @param startTimeStamp 起始时间戳（毫秒）
     * @param appointmentTimes 已预约的时间段
     * @param needCheck 是否需要检查时间
     * @return 最大可预约时间（分钟）
     */
    private int calculateMaxAvailableTime(Long startTimeStamp, List<Map<String, String>> appointmentTimes, boolean needCheck) {
        // 排序预约时间段
        appointmentTimes.sort((a, b) -> {
            String startA = a.get("start");
            String startB = b.get("start");
            
            try {
                // 尝试解析时间戳格式
                long timestampA = Long.parseLong(startA);
                long timestampB = Long.parseLong(startB);
                return Long.compare(timestampA, timestampB);
            } catch (Exception e) {
                return 0; // 解析失败，保持原有顺序
            }
        });
        
        // 计算最大可预约时间
        long maxAvailableMinutes = 99999; // 默认最大可预约时间
        
        // 转换请求时间戳为秒级
        long requestTimestampSec = startTimeStamp / 1000;
        
        for (Map<String, String> timeSlot : appointmentTimes) {
            String slotStart = timeSlot.get("start");
            String slotEnd = timeSlot.get("end");
            
            try {
                // 解析预约开始和结束时间戳（秒级）
                long slotStartSec = Long.parseLong(slotStart);
                long slotEndSec = Long.parseLong(slotEnd);
                
                // 检查当前请求时间是否在预约时间段内
                if (requestTimestampSec >= slotStartSec && requestTimestampSec < slotEndSec) {
                    return 0; // 当前时间已被预约
                }
                
                // 检查是否需要预留检查时间
                if (needCheck && requestTimestampSec >= slotEndSec && requestTimestampSec < slotEndSec + 1800) { // 1800秒 = 30分钟
                    return 0; // 处于检查时间内
                }
                
                // 计算可预约时间
                if (requestTimestampSec < slotStartSec) {
                    // 计算从请求时间到下一个预约开始的时间差（秒）
                    long availableSeconds = slotStartSec - requestTimestampSec;
                    
                    // 考虑检查时间
                    if (needCheck) {
                        // 查找前一个预约的结束时间
                        long previousEndSec = 0;
                        for (Map<String, String> prevSlot : appointmentTimes) {
                            long prevEnd = Long.parseLong(prevSlot.get("end"));
                            if (prevEnd < requestTimestampSec) {
                                previousEndSec = prevEnd;
                            } else {
                                break;
                            }
                        }
                        
                        // 如果当前时间在检查时间内，不可预约
                        if (previousEndSec > 0 && requestTimestampSec < previousEndSec + 1800) {
                            return 0;
                        }
                        
                        // 对于需要检查的资源，预约结束后需要预留30分钟检查时间，所以可预约时间应在下一次预约开始前30分钟结束
                        availableSeconds -= 1800; // 减去30分钟（1800秒）的检查时间
                        if (availableSeconds <= 0) {
                            return 0; // 如果减去检查时间后没有可预约时间，返回0
                        }
                    }
                    
                    // 转换为分钟并返回
                    maxAvailableMinutes = availableSeconds / 60;
                    break;
                }
            } catch (Exception e) {
                // 解析失败，跳过该记录
            }
        }
        
        return (int) maxAvailableMinutes;
    }
    
    /**
     * 获取单个资源的所有不可预约时间API
     * @param request 请求体，包含data字段
     * @return 不可预约的时间列表
     */
    @PostMapping("/unavailable-times")
    public ResponseEntity<Map<String, Object>> getUnavailableTimes(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("获取不可预约时间失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("获取不可预约时间失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("获取不可预约时间失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("type")) {
                logger.error("获取不可预约时间失败：data字段缺少type");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("id")) {
                logger.error("获取不可预约时间失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("day")) {
                logger.error("获取不可预约时间失败：data字段缺少day");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证type字段
            String type = data.get("type") instanceof String ? (String) data.get("type") : null;
            if (type == null || (!type.equals("space") && !type.equals("physical"))) {
                logger.error("获取不可预约时间失败：type字段无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    id = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.error("获取不可预约时间失败：id不是有效的数字");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (id == null) {
                logger.error("获取不可预约时间失败：id参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证day字段
            Long dayTimestamp = null;
            Object dayObj = data.get("day");
            if (dayObj instanceof Number) {
                dayTimestamp = ((Number) dayObj).longValue();
            } else if (dayObj instanceof String) {
                try {
                    dayTimestamp = Long.parseLong((String) dayObj);
                } catch (NumberFormatException e) {
                    logger.error("获取不可预约时间失败：day不是有效的时间戳");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (dayTimestamp == null) {
                logger.error("获取不可预约时间失败：day参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证day是否为0点的时间戳
            Date dayDate = new Date(dayTimestamp * 1000);
            if (dayDate.getHours() != 0 || dayDate.getMinutes() != 0 || dayDate.getSeconds() != 0) {
                logger.error("获取不可预约时间失败：day必须是0点的时间戳");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 计算当天的开始和结束时间戳（秒级）
            long dayStartSec = dayTimestamp;
            long dayEndSec = dayStartSec + 86400; // 86400秒 = 24小时

            // 查询预约表中该资源的预约记录
            List<Appointment> appointments = appointmentRepository.findAll();
            List<Map<String, Object>> unavailableTimes = new ArrayList<>();
            int count = 1;

            // 检查资源是否需要检查
            boolean needCheck = false;
            if (type.equals("space")) {
                SpaceResource spaceResource = spaceResourceRepository.findById(id).orElse(null);
                if (spaceResource != null && spaceResource.getCheckFlag() == 1) {
                    needCheck = true;
                }
            } else if (type.equals("physical")) {
                PhysicalResource physicalResource = physicalResourceRepository.findById(id).orElse(null);
                if (physicalResource != null && physicalResource.getCheckFlag() == 1) {
                    needCheck = true;
                }
            }

            for (Appointment appointment : appointments) {
                if (appointment.getType().equals(type) && appointment.getResId().equals(id)) {
                    try {
                        // 解析预约开始和结束时间戳（秒级）
                        long appStartSec = Long.parseLong(appointment.getStartTime());
                        long appEndSec = Long.parseLong(appointment.getEndTime());

                        // 检查预约是否与当天有交集
                        if (appStartSec < dayEndSec && appEndSec > dayStartSec) {
                            // 计算当天的预约时间段
                            long startSec = Math.max(appStartSec, dayStartSec);
                            long endSec = Math.min(appEndSec, dayEndSec);

                            // 检查是否为截断时间
                            boolean isCut = (appStartSec < dayStartSec || appEndSec > dayEndSec);

                            // 添加预约时间
                            Map<String, Object> timeSlot = new LinkedHashMap<>();
                            timeSlot.put("id", count++);
                            timeSlot.put("type", "appointment");
                            timeSlot.put("start_time", startSec);
                            timeSlot.put("end_time", endSec);
                            timeSlot.put("iscut", isCut);
                            unavailableTimes.add(timeSlot);
                        }

                        // 如果需要检查，添加检查时间（无论预约是否与当天有交集）
                        if (needCheck) {
                            // 预约开始前的检查时间
                            long checkStartBefore = appStartSec - 1800; // 1800秒 = 30分钟
                            long checkEndBefore = appStartSec;

                            // 检查是否与当天有交集
                            if (checkStartBefore < dayEndSec && checkEndBefore > dayStartSec) {
                                long checkStart = Math.max(checkStartBefore, dayStartSec);
                                long checkEnd = Math.min(checkEndBefore, dayEndSec);
                                boolean isCutBefore = (checkStartBefore < dayStartSec || checkEndBefore > dayEndSec);

                                Map<String, Object> checkSlotBefore = new LinkedHashMap<>();
                                checkSlotBefore.put("id", count++);
                                checkSlotBefore.put("type", "check");
                                checkSlotBefore.put("start_time", checkStart);
                                checkSlotBefore.put("end_time", checkEnd);
                                checkSlotBefore.put("iscut", isCutBefore);
                                unavailableTimes.add(checkSlotBefore);
                            }

                            // 预约结束后的检查时间
                            long checkStartAfter = appEndSec;
                            long checkEndAfter = appEndSec + 1800; // 1800秒 = 30分钟

                            // 检查是否与当天有交集
                            if (checkStartAfter < dayEndSec && checkEndAfter > dayStartSec) {
                                long checkStart = Math.max(checkStartAfter, dayStartSec);
                                long checkEnd = Math.min(checkEndAfter, dayEndSec);
                                boolean isCutAfter = (checkStartAfter < dayStartSec || checkEndAfter > dayEndSec);

                                Map<String, Object> checkSlotAfter = new LinkedHashMap<>();
                                checkSlotAfter.put("id", count++);
                                checkSlotAfter.put("type", "check");
                                checkSlotAfter.put("start_time", checkStart);
                                checkSlotAfter.put("end_time", checkEnd);
                                checkSlotAfter.put("iscut", isCutAfter);
                                unavailableTimes.add(checkSlotAfter);
                            }
                        }
                    } catch (Exception e) {
                        continue; // 解析失败，跳过该记录
                    }
                }
            }
            
            // 按开始时间排序
            unavailableTimes.sort((a, b) -> {
                long startA = (long) a.get("start_time");
                long startB = (long) b.get("start_time");
                return Long.compare(startA, startB);
            });
            
            // 构造响应
            response.put("success", true);
            response.put("message", "获取不可预约时间成功");
            response.put("data", unavailableTimes);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("获取不可预约时间失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取不可预约时间失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取不可预约时间失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取不可预约时间失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}