package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.entity.Appointment;
import com.orangelan.resolinkagent.entity.PhysicalResource;
import com.orangelan.resolinkagent.entity.SpaceResource;
import com.orangelan.resolinkagent.entity.AdminAccount;
import com.orangelan.resolinkagent.entity.UserAccount;
import com.orangelan.resolinkagent.entity.UserInfo;
import com.orangelan.resolinkagent.repository.AdminAccountRepository;
import com.orangelan.resolinkagent.repository.AppointmentRepository;
import com.orangelan.resolinkagent.repository.PhysicalResourceRepository;
import com.orangelan.resolinkagent.repository.SpaceResourceRepository;
import com.orangelan.resolinkagent.repository.UserInfoRepository;
import com.orangelan.resolinkagent.repository.UserAccountRepository;
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
public class AddAppointmentToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(AddAppointmentToolService.class);
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private PhysicalResourceRepository physicalResourceRepository;
    
    @Autowired
    private SpaceResourceRepository spaceResourceRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private AdminAccountRepository adminAccountRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取添加预约记录工具的定义
     * @return 工具定义
     */
    public Map<String, Object> getAddAppointmentToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "add_appointment");
        function.put("description", "添加预约记录");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> typeParam = new HashMap<>();
        typeParam.put("type", "string");
        typeParam.put("description", "资源类型，\"space\"表示空间资源，\"physical\"表示实物资源");
        properties.put("type", typeParam);
        
        Map<String, Object> idParam = new HashMap<>();
        idParam.put("type", "integer");
        idParam.put("description", "对应类型的资源ID");
        properties.put("id", idParam);
        
        Map<String, Object> userAccountParam = new HashMap<>();
        userAccountParam.put("type", "string");
        userAccountParam.put("description", "预约人账号");
        properties.put("user_account", userAccountParam);
        
        Map<String, Object> startTimeParam = new HashMap<>();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "预约开始时间，格式为YYYY-MM-DD-HH:MM:SS");
        properties.put("start_time", startTimeParam);
        
        Map<String, Object> endTimeParam = new HashMap<>();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "预约结束时间，格式为YYYY-MM-DD-HH:MM:SS");
        properties.put("end_time", endTimeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"type", "id", "user_account", "start_time", "end_time"});
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }
    
    /**
     * 添加预约记录
     * @param type 资源类型
     * @param id 资源ID
     * @param userAccount 预约人账号
     * @param startTime 开始时间，格式为YYYY-MM-DD-HH:MM:SS
     * @param endTime 结束时间，格式为YYYY-MM-DD-HH:MM:SS
     * @return 添加结果
     */
    public String addAppointment(String type, Long id, String userAccount, String startTime, String endTime) {
        try {
            logger.info("添加预约记录，资源类型：{}，资源ID：{}，用户账号：{}，开始时间：{}，结束时间：{}", 
                    type, id, userAccount, startTime, endTime);
            
            // 验证时间格式
            if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
                return "时间格式错误，请使用YYYY-MM-DD-HH:MM:SS格式";
            }
            
            // 将时间字符串转换为秒级时间戳
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            Date startDate = dateFormat.parse(startTime);
            Date endDate = dateFormat.parse(endTime);
            long startTimeStamp = startDate.getTime() / 1000; // 转换为秒级时间戳
            long endTimeStamp = endDate.getTime() / 1000;
            
            // 验证时间逻辑
            if (startTimeStamp >= endTimeStamp) {
                return "开始时间不能晚于或等于结束时间";
            }
            
            // 根据资源类型查找资源名称
            String resourceName = getResourceName(type, id);
            if (resourceName == null) {
                return "未找到对应的资源，请检查资源类型和ID是否正确";
            }
            
            // 根据用户账号查找用户信息
            Map<String, String> userInfo = getUserInfoByAccount(userAccount);
            if (userInfo == null) {
                return "未找到对应的用户信息，请检查用户账号是否正确";
            }
            
            // 获取当前时间作为预约日期
            String appointmentDate = dateFormat.format(new Date());
            
            // 创建预约记录
            Appointment appointment = new Appointment();
            appointment.setEmpId(userInfo.get("empId"));
            appointment.setName(userInfo.get("name"));
            appointment.setResId(id);
            appointment.setResName(resourceName);
            appointment.setType(type);
            appointment.setAppointmentDate(dateFormat.parse(appointmentDate));
            appointment.setStartTime(String.valueOf(startTimeStamp));
            appointment.setEndTime(String.valueOf(endTimeStamp));
            appointment.setStatus("0"); // 状态为0表示正常
            
            // 保存预约记录
            appointmentRepository.save(appointment);
            
            logger.info("预约记录添加成功，预约ID：{}", appointment.getId());
            
            return String.format("预约记录添加成功！预约ID：%d，资源：%s，预约人：%s，时间：%s 至 %s", 
                    appointment.getId(), resourceName, userInfo.get("name"), startTime, endTime);
            
        } catch (ParseException e) {
            logger.error("时间格式解析错误：{}", e.getMessage());
            return "时间格式错误，请使用YYYY-MM-DD-HH:MM:SS格式";
        } catch (Exception e) {
            logger.error("添加预约记录时发生错误：{}", e.getMessage());
            return "添加预约记录失败：" + e.getMessage();
        }
    }
    
    /**
     * 验证时间格式是否正确
     * @param time 时间字符串
     * @return 是否有效
     */
    private boolean isValidTimeFormat(String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            dateFormat.setLenient(false); // 严格模式
            dateFormat.parse(time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
    
    /**
     * 根据资源类型和ID获取资源名称
     * @param type 资源类型
     * @param id 资源ID
     * @return 资源名称
     */
    private String getResourceName(String type, Long id) {
        try {
            if ("space".equals(type)) {
                Optional<SpaceResource> spaceResource = spaceResourceRepository.findById(id);
                return spaceResource.map(SpaceResource::getSpaceName).orElse(null);
            } else if ("physical".equals(type)) {
                Optional<PhysicalResource> physicalResource = physicalResourceRepository.findById(id);
                return physicalResource.map(PhysicalResource::getEquipmentName).orElse(null);
            }
            return null;
        } catch (Exception e) {
            logger.error("获取资源名称时发生错误：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据用户账号获取用户信息
     * @param userAccount 用户账号
     * @return 用户信息（包含empId和name）
     */
    private Map<String, String> getUserInfoByAccount(String userAccount) {
        try {
            // 首先检查是否是Admin用户
            Optional<AdminAccount> adminAccountOpt = adminAccountRepository.findByAdacc(userAccount);
            if (adminAccountOpt.isPresent()) {
                String empId = adminAccountOpt.get().getEmpId();
                
                // 根据emp_id查找用户信息（Admin用户也从UserInfo表获取姓名）
                Optional<UserInfo> userInfo = userInfoRepository.findByEmpId(empId);
                if (userInfo.isPresent()) {
                    Map<String, String> result = new HashMap<>();
                    result.put("empId", empId);
                    result.put("name", userInfo.get().getName());
                    return result;
                }
            }
            
            // 如果不是Admin用户，检查普通用户
            Optional<UserAccount> userAccountOpt = userAccountRepository.findByUseracc(userAccount);
            if (userAccountOpt.isPresent()) {
                String empId = userAccountOpt.get().getEmpId();
                
                // 根据emp_id查找用户信息
                Optional<UserInfo> userInfo = userInfoRepository.findByEmpId(empId);
                if (userInfo.isPresent()) {
                    Map<String, String> result = new HashMap<>();
                    result.put("empId", empId);
                    result.put("name", userInfo.get().getName());
                    return result;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("获取用户信息时发生错误：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据用户账号查找对应的emp_id
     * @param userAccount 用户账号
     * @return emp_id
     */
    private String findEmpIdByUserAccount(String userAccount) {
        try {
            // 首先检查是否是Admin用户
            Optional<AdminAccount> adminAccountOpt = adminAccountRepository.findByAdacc(userAccount);
            if (adminAccountOpt.isPresent()) {
                return adminAccountOpt.get().getEmpId();
            }
            
            // 如果不是Admin用户，检查普通用户
            Optional<UserAccount> userAccountOpt = userAccountRepository.findByUseracc(userAccount);
            if (userAccountOpt.isPresent()) {
                return userAccountOpt.get().getEmpId();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("根据用户账号查找emp_id时发生错误：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理工具调用的函数映射
     * @return 函数名到实际执行函数的映射
     */
    public Map<String, Function<Map<String, Object>, String>> getToolFunctions() {
        Map<String, Function<Map<String, Object>, String>> functions = new HashMap<>();
        
        // 添加预约记录函数（支持中英文函数名）
        Function<Map<String, Object>, String> addAppointmentFunction = params -> {
            String type = params.containsKey("type") ? (String) params.get("type") : null;
            Long id = null;
            if (params.containsKey("id")) {
                id = ((Number) params.get("id")).longValue();
            }
            String userAccount = params.containsKey("user_account") ? (String) params.get("user_account") : null;
            String startTime = params.containsKey("start_time") ? (String) params.get("start_time") : null;
            String endTime = params.containsKey("end_time") ? (String) params.get("end_time") : null;
            
            return addAppointment(type, id, userAccount, startTime, endTime);
        };
        
        functions.put("add_appointment", addAppointmentFunction);
        functions.put("add_reservation", addAppointmentFunction);
        
        return functions;
    }
}