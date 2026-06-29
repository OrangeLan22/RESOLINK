package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.PhysicalResource;
import com.orangelan.resolinkserver.entity.SpaceResource;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.repository.AdminAccountRepository;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminAccountRepository adminAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppointmentRepository appointmentRepository;
    private final PhysicalResourceRepository physicalResourceRepository;
    private final SpaceResourceRepository spaceResourceRepository;
    
    public HomeController(JwtTokenUtil jwtTokenUtil, AdminAccountRepository adminAccountRepository, 
                         UserAccountRepository userAccountRepository, AppointmentRepository appointmentRepository, 
                         PhysicalResourceRepository physicalResourceRepository, SpaceResourceRepository spaceResourceRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.adminAccountRepository = adminAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.appointmentRepository = appointmentRepository;
        this.physicalResourceRepository = physicalResourceRepository;
        this.spaceResourceRepository = spaceResourceRepository;
    }
    
    /**
     * 获取本人预约记录
     * @param requestBody 请求体，包含token字段
     * @return 预约记录列表
     */
    @PostMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> getMyAppointments(@RequestBody Map<String, String> requestBody) {
        try {
            logger.info("获取本人预约记录");
            
            // 获取token
            String token = requestBody.get("token");
            if (token == null || token.trim().isEmpty()) {
                logger.error("请求体中未找到Token");
                return ResponseEntity.badRequest().build();
            }
            
            // 验证token
            if (!jwtTokenUtil.validateToken(token)) {
                logger.error("Token无效或已过期");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 从token中获取用户名
            String username = jwtTokenUtil.getUsernameFromToken(token);
            logger.info("从Token中获取的用户名：{}", username);
            
            // 查找empId
            String empId = null;
            
            // 先在adminaccount表中查找
            AdminAccount adminAccount = adminAccountRepository.findByAdacc(username);
            if (adminAccount != null) {
                empId = adminAccount.getEmpId();
                logger.info("在adminaccount表中找到empId：{}", empId);
            } else {
                // 在useraccount表中查找
                UserAccount userAccount = userAccountRepository.findByUseracc(username);
                if (userAccount != null) {
                    empId = userAccount.getEmpId();
                    logger.info("在useraccount表中找到empId：{}", empId);
                } else {
                    logger.error("用户不存在");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            }
            
            // 查询该empId的所有预约记录
            List<Appointment> appointments = appointmentRepository.findByEmpId(empId);
            logger.info("找到 {} 条预约记录", appointments.size());
            
            // 过滤掉已经结束的预约记录，只返回正在进行中和未开始的
            List<Map<String, Object>> result = new ArrayList<>();
            long currentTime = System.currentTimeMillis() / 1000; // 转换为秒级时间戳
            
            for (Appointment appointment : appointments) {
                try {
                    // 解析开始时间和结束时间为秒级时间戳
                    long startTime = Long.parseLong(appointment.getStartTime());
                    long endTime = Long.parseLong(appointment.getEndTime());
                    
                    // 如果当前时间在结束时间之前，且预约状态不是已取消，则添加到结果中
                    if (currentTime < endTime && !"-1".equals(appointment.getStatus())) {
                        Map<String, Object> appointmentMap = new HashMap<>();
                        String resourceType = appointment.getType();
                        Long resId = appointment.getResId();
                        String location = "";
                        
                        // 根据资源类型查询对应的资源表获取location值
                        if ("space".equals(resourceType)) {
                            // 查询空间资源表
                            SpaceResource spaceResource = spaceResourceRepository.findById(resId).orElse(null);
                            if (spaceResource != null) {
                                location = spaceResource.getLocation();
                                logger.info("从空间资源表获取location：{}", location);
                            }
                        } else if ("physical".equals(resourceType)) {
                            // 查询物理资源表
                            PhysicalResource physicalResource = physicalResourceRepository.findById(resId).orElse(null);
                            if (physicalResource != null) {
                                location = physicalResource.getLocation();
                                logger.info("从物理资源表获取location：{}", location);
                            }
                        }
                        
                        appointmentMap.put("id", appointment.getId());
                        appointmentMap.put("resourceType", resourceType);
                        appointmentMap.put("resourceId", resId);
                        appointmentMap.put("resourceName", appointment.getResName());
                        appointmentMap.put("startTime", startTime);
                        appointmentMap.put("endTime", endTime);
                        appointmentMap.put("location", location);
                        result.add(appointmentMap);
                    }
                } catch (NumberFormatException e) {
                    logger.error("解析时间戳失败：{}", e.getMessage());
                    continue;
                }
            }
            
            logger.info("过滤后返回 {} 条预约记录", result.size());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.error("获取预约记录失败：业务逻辑错误", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("获取预约记录失败：系统错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}