package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.Department;
import com.orangelan.resolinkserver.entity.Status;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.repository.StatusRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
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
 * 历史记录控制器
 * 用于处理历史记录相关的API请求
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    
    private final AppointmentRepository appointmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserInfoRepository userInfoRepository;
    private final DepartmentRepository departmentRepository;
    private final StatusRepository statusRepository;
    private final JwtTokenUtil jwtTokenUtil;
    
    public HistoryController(AppointmentRepository appointmentRepository, 
                            UserAccountRepository userAccountRepository, UserInfoRepository userInfoRepository,
                            DepartmentRepository departmentRepository, StatusRepository statusRepository,
                            JwtTokenUtil jwtTokenUtil) {
        this.appointmentRepository = appointmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.userInfoRepository = userInfoRepository;
        this.departmentRepository = departmentRepository;
        this.statusRepository = statusRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    /**
     * 获取历史记录列表API
     * @param request 请求体，包含查询条件和分页信息
     * @return 历史记录列表
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getHistoryList(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("获取历史记录列表失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("获取历史记录列表失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("获取历史记录列表失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 获取分页参数
            int page = data.containsKey("page") ? (Integer) data.get("page") : 1;
            int size = data.containsKey("size") ? (Integer) data.get("size") : 10;
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, size);
            
            // 查询预约记录（历史记录）
            Page<Appointment> appointmentPage = appointmentRepository.findAll(pageable);
            List<Appointment> appointmentList = appointmentPage.getContent();
            
            // 补充预约记录的部门和状态信息
            List<Appointment> resultAppointmentList = new ArrayList<>();
            for (Appointment appointment : appointmentList) {
                // 获取用户信息
                Optional<UserAccount> userAccountOptional = Optional.ofNullable(userAccountRepository.findByEmpId(appointment.getEmpId()));
                if (userAccountOptional.isPresent()) {
                    UserAccount userAccount = userAccountOptional.get();
                    Optional<UserInfo> userInfoOptional = userInfoRepository.findById(userAccount.getId());
                    if (userInfoOptional.isPresent()) {
                        UserInfo userInfo = userInfoOptional.get();
                        // 获取部门信息
                        Optional<Department> departmentOptional = departmentRepository.findById(userInfo.getDepId());
                        if (departmentOptional.isPresent()) {
                            appointment.setDepName(departmentOptional.get().getDepName());
                        }
                    }
                }
                
                // 获取状态信息
                Status statusEntity = statusRepository.findByStaName(appointment.getStatus());
                if (statusEntity != null) {
                    appointment.setStaName(statusEntity.getStaName());
                }
                
                resultAppointmentList.add(appointment);
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "获取历史记录列表成功");
            response.put("data", resultAppointmentList);
            response.put("total", appointmentPage.getTotalElements());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("获取历史记录列表失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("获取历史记录列表失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取历史记录列表失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取历史记录列表失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取历史记录列表失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据条件查询历史记录API
     * @param request 请求体，包含查询条件和分页信息
     * @return 符合条件的历史记录列表
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchHistory(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("查询历史记录失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("查询历史记录失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("查询历史记录失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 获取分页参数
            int page = data.containsKey("page") ? (Integer) data.get("page") : 1;
            int size = data.containsKey("size") ? (Integer) data.get("size") : 10;
            
            // 获取查询条件
            String empId = data.containsKey("emp_id") ? (String) data.get("emp_id") : null;
            Long resId = data.containsKey("res_id") ? ((Number) data.get("res_id")).longValue() : null;
            String type = data.containsKey("type") ? (String) data.get("type") : null;
            String statusValue = data.containsKey("status") ? (String) data.get("status") : null;
            
            // 获取预约日期范围条件
            Date appointmentDateStart = null;
            Date appointmentDateEnd = null;
            if (data.containsKey("appointment_date_start") && data.containsKey("appointment_date_end")) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    appointmentDateStart = dateFormat.parse((String) data.get("appointment_date_start"));
                    appointmentDateEnd = dateFormat.parse((String) data.get("appointment_date_end"));
                    // 设置结束时间为当天的23:59:59
                    appointmentDateEnd = new Date(appointmentDateEnd.getTime() + 24 * 60 * 60 * 1000 - 1);
                } catch (Exception e) {
                    logger.error("查询历史记录失败：预约日期格式错误");
                    response.put("success", false);
                    response.put("message", "预约日期格式错误，请使用yyyy-MM-dd格式");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 根据条件查询预约记录（历史记录）
            List<Appointment> appointmentList = new ArrayList<>();
            
            // 如果有预约日期范围条件，优先使用日期范围查询
            if (appointmentDateStart != null && appointmentDateEnd != null) {
                if (empId != null) {
                    appointmentList = appointmentRepository.findByEmpIdAndAppointmentDateBetween(empId, appointmentDateStart, appointmentDateEnd);
                } else if (resId != null) {
                    appointmentList = appointmentRepository.findByResIdAndAppointmentDateBetween(resId, appointmentDateStart, appointmentDateEnd);
                } else {
                    appointmentList = appointmentRepository.findByAppointmentDateBetween(appointmentDateStart, appointmentDateEnd);
                }
            } else {
                // 没有日期范围条件，根据其他条件查询
                if (empId != null) {
                    appointmentList = appointmentRepository.findByEmpId(empId);
                } else if (resId != null) {
                    appointmentList = appointmentRepository.findByResId(resId);
                } else if (type != null) {
                    appointmentList = appointmentRepository.findByType(type);
                } else if (statusValue != null) {
                    appointmentList = appointmentRepository.findByStatus(statusValue);
                } else {
                    // 没有任何条件，查询所有预约记录
                    appointmentList = appointmentRepository.findAll();
                }
            }
            
            // 进一步过滤结果
            List<Appointment> filteredAppointmentList = new ArrayList<>();
            for (Appointment appointment : appointmentList) {
                boolean match = true;
                
                if (type != null && !appointment.getType().equals(type)) {
                    match = false;
                }
                
                if (statusValue != null && !appointment.getStatus().equals(statusValue)) {
                    match = false;
                }
                
                if (match) {
                    // 获取用户信息
                    Optional<UserAccount> userAccountOptional = Optional.ofNullable(userAccountRepository.findByEmpId(appointment.getEmpId()));
                    if (userAccountOptional.isPresent()) {
                        UserAccount userAccount = userAccountOptional.get();
                        Optional<UserInfo> userInfoOptional = userInfoRepository.findById(userAccount.getId());
                        if (userInfoOptional.isPresent()) {
                            UserInfo userInfo = userInfoOptional.get();
                            // 获取部门信息
                            Optional<Department> departmentOptional = departmentRepository.findById(userInfo.getDepId());
                            if (departmentOptional.isPresent()) {
                                appointment.setDepName(departmentOptional.get().getDepName());
                            }
                        }
                    }
                    
                    // 获取状态信息
                    Status statusEntity = statusRepository.findByStaName(appointment.getStatus());
                    if (statusEntity != null) {
                        appointment.setStaName(statusEntity.getStaName());
                    }
                    
                    filteredAppointmentList.add(appointment);
                }
            }
            
            // 分页处理
            int total = filteredAppointmentList.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);
            
            List<Appointment> pagedAppointmentList = new ArrayList<>();
            if (startIndex < total) {
                pagedAppointmentList = filteredAppointmentList.subList(startIndex, endIndex);
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "查询历史记录成功");
            response.put("data", pagedAppointmentList);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("查询历史记录失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("查询历史记录失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "查询历史记录失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("查询历史记录失败：系统错误", e);
            response.put("success", false);
            response.put("message", "查询历史记录失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取历史记录详情API
     * @param request 请求体，包含历史记录ID
     * @return 历史记录详情
     */
    @PostMapping("/detail")
    public ResponseEntity<Map<String, Object>> getHistoryDetail(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("获取历史记录详情失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("获取历史记录详情失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("获取历史记录详情失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("获取历史记录详情失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取id字段
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    id = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.error("获取历史记录详情失败：id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (id == null) {
                logger.error("获取历史记录详情失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 查询预约记录（历史记录）
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);
            if (!appointmentOptional.isPresent()) {
                logger.error("获取历史记录详情失败：历史记录不存在，ID：{}", id);
                response.put("success", false);
                response.put("message", "历史记录不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // 获取用户信息
            Optional<UserAccount> userAccountOptional = Optional.ofNullable(userAccountRepository.findByEmpId(appointment.getEmpId()));
            if (userAccountOptional.isPresent()) {
                UserAccount userAccount = userAccountOptional.get();
                Optional<UserInfo> userInfoOptional = userInfoRepository.findById(userAccount.getId());
                if (userInfoOptional.isPresent()) {
                    UserInfo userInfo = userInfoOptional.get();
                    // 获取部门信息
                    Optional<Department> departmentOptional = departmentRepository.findById(userInfo.getDepId());
                    if (departmentOptional.isPresent()) {
                        appointment.setDepName(departmentOptional.get().getDepName());
                    }
                }
            }
            
            // 获取状态信息
            Status statusEntity = statusRepository.findByStaName(appointment.getStatus());
            if (statusEntity != null) {
                appointment.setStaName(statusEntity.getStaName());
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "获取历史记录详情成功");
            response.put("data", appointment);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("获取历史记录详情失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("获取历史记录详情失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取历史记录详情失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取历史记录详情失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取历史记录详情失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}