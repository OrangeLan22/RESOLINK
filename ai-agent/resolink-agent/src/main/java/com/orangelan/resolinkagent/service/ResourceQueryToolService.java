package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.entity.AdminAccount;
import com.orangelan.resolinkagent.entity.PhysicalResource;
import com.orangelan.resolinkagent.entity.SpaceResource;
import com.orangelan.resolinkagent.entity.UserInfo;
import com.orangelan.resolinkagent.repository.AdminAccountRepository;
import com.orangelan.resolinkagent.repository.PhysicalResourceRepository;
import com.orangelan.resolinkagent.repository.SpaceResourceRepository;
import com.orangelan.resolinkagent.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class ResourceQueryToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceQueryToolService.class);
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private AdminAccountRepository adminAccountRepository;
    
    @Autowired
    private PhysicalResourceRepository physicalResourceRepository;
    
    @Autowired
    private SpaceResourceRepository spaceResourceRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取资源查询工具的定义
     * @return 工具定义
     */
    public Map<String, Object> getResourceQueryToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "query_resources");
        function.put("description", "查询可用资源，包括物理资源和空间资源");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> typeParam = new HashMap<>();
        typeParam.put("type", "string");
        typeParam.put("description", "资源类型，空间类型传入\"space\"，物理资源类型传入\"physical\"");
        properties.put("type", typeParam);
        
        Map<String, Object> userAccountParam = new HashMap<>();
        userAccountParam.put("type", "string");
        userAccountParam.put("description", "用户账号，用于查询用户权限范围内的资源");
        properties.put("user_account", userAccountParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"type", "user_account"});
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }
    
    /**
     * 执行资源查询
     * @param type 资源类型
     * @param userAccount 用户账号
     * @return 查询结果
     */
    public String queryResources(String type, String userAccount) {
        try {
            logger.info("查询资源，类型：{}，用户账号：{}", type, userAccount);
            
            // 检查用户是否为管理员
            boolean isAdmin = checkIfAdmin(userAccount);
            String userDepId = null;
            
            if (!isAdmin) {
                // 获取用户部门ID
                userDepId = getUserDepartmentId(userAccount);
                if (userDepId == null) {
                    return "无法获取用户部门信息，请检查用户账号是否正确";
                }
            }
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("type", type);
            result.put("user_account", userAccount);
            result.put("is_admin", isAdmin);
            if (!isAdmin) {
                result.put("department_id", userDepId);
            }
            
            // 根据type参数查询对应的资源
            if ("physical".equalsIgnoreCase(type)) {
                // 查询物理资源
                List<PhysicalResource> physicalResources = new ArrayList<>();
                if (isAdmin) {
                    // 管理员可以查询所有可用的物理资源
                    physicalResources = physicalResourceRepository.findAllByStageNot(-1);
                } else {
                    // 普通用户只能查询公开的或本部门的物理资源
                    physicalResources.addAll(physicalResourceRepository.findPublicResources());
                    physicalResources.addAll(physicalResourceRepository.findDepartmentResources(userDepId));
                }
                
                // 添加物理资源信息
                List<Map<String, Object>> physicalResourceList = new ArrayList<>();
                for (PhysicalResource resource : physicalResources) {
                    Map<String, Object> resourceInfo = new HashMap<>();
                    resourceInfo.put("id", resource.getId());
                    resourceInfo.put("name", resource.getEquipmentName());
                    resourceInfo.put("location", resource.getLocation());
                    resourceInfo.put("type", resource.getType());
                    resourceInfo.put("public", resource.getPublicFlag() == 1 ? "是" : "否");
                    resourceInfo.put("department_id", resource.getDepId());
                    resourceInfo.put("check", resource.getCheckFlag() == 1 ? "是" : "否");
                    resourceInfo.put("note", resource.getNote());
                    resourceInfo.put("stage", resource.getStage());
                    resourceInfo.put("resource_category", "物理资源");
                    
                    physicalResourceList.add(resourceInfo);
                }
                result.put("resources", physicalResourceList);
            } else if ("space".equalsIgnoreCase(type)) {
                // 查询空间资源
                List<SpaceResource> spaceResources = new ArrayList<>();
                if (isAdmin) {
                    // 管理员可以查询所有可用的空间资源
                    spaceResources = spaceResourceRepository.findAllByStageNot(-1);
                } else {
                    // 普通用户只能查询公开的或本部门的空间资源
                    spaceResources.addAll(spaceResourceRepository.findPublicResources());
                    spaceResources.addAll(spaceResourceRepository.findDepartmentResources(userDepId));
                }
                
                // 添加空间资源信息
                List<Map<String, Object>> spaceResourceList = new ArrayList<>();
                for (SpaceResource resource : spaceResources) {
                    Map<String, Object> resourceInfo = new HashMap<>();
                    resourceInfo.put("id", resource.getId());
                    resourceInfo.put("name", resource.getSpaceName());
                    resourceInfo.put("location", resource.getLocation());
                    resourceInfo.put("type", resource.getType());
                    resourceInfo.put("capacity", resource.getCapacity());
                    resourceInfo.put("public", resource.getPublicFlag() == 1 ? "是" : "否");
                    resourceInfo.put("department_id", resource.getDepId());
                    resourceInfo.put("check", resource.getCheckFlag() == 1 ? "是" : "否");
                    resourceInfo.put("note", resource.getNote());
                    resourceInfo.put("stage", resource.getStage());
                    resourceInfo.put("resource_category", "空间资源");
                    
                    spaceResourceList.add(resourceInfo);
                }
                result.put("resources", spaceResourceList);
            } else {
                return "无效的资源类型，请使用 'physical' 或 'space'";
            }
            
            // 添加字段说明
            Map<String, String> fieldDescriptions = new HashMap<>();
            fieldDescriptions.put("id", "资源唯一标识");
            fieldDescriptions.put("name", "资源名称（物理资源为设备名称，空间资源为空间名称）");
            fieldDescriptions.put("location", "资源位置");
            fieldDescriptions.put("type", "资源类型");
            fieldDescriptions.put("capacity", "资源容量（仅空间资源有此字段）");
            fieldDescriptions.put("public", "是否为公开资源（是表示所有用户均可预约，否表示仅特定部门可预约）");
            fieldDescriptions.put("department_id", "可预约此资源的部门ID（多个部门ID用逗号分隔）");
            fieldDescriptions.put("check", "归还时是否需要检查（是表示归还时需要检查资源是否正常，否表示可直接归还）");
            fieldDescriptions.put("note", "资源备注信息");
            fieldDescriptions.put("stage", "资源状态（-1表示不可用，0表示可用）");
            fieldDescriptions.put("resource_category", "资源类别（物理资源或空间资源）");
            result.put("field_descriptions", fieldDescriptions);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            logger.error("查询资源失败", e);
            return "查询资源失败：" + e.getMessage();
        }
    }
    
    /**
     * 检查用户是否为管理员
     * @param userAccount 用户账号
     * @return 是否为管理员
     */
    private boolean checkIfAdmin(String userAccount) {
        Optional<AdminAccount> adminAccount = adminAccountRepository.findByAdacc(userAccount);
        return adminAccount.isPresent();
    }
    
    /**
     * 获取用户部门ID
     * @param userAccount 用户账号
     * @return 部门ID
     */
    private String getUserDepartmentId(String userAccount) {
        Optional<UserInfo> userInfo = userInfoRepository.findByUseracc(userAccount);
        if (userInfo.isPresent()) {
            return userInfo.get().getDepId().toString();
        }
        return null;
    }
    
    /**
     * 处理工具调用的函数映射
     * @return 函数名到实际执行函数的映射
     */
    public Map<String, Function<Map<String, Object>, String>> getToolFunctions() {
        Map<String, Function<Map<String, Object>, String>> functions = new HashMap<>();
        
        // 添加资源查询函数
        functions.put("query_resources", params -> {
            String type = params.containsKey("type") ? (String) params.get("type") : null;
            String userAccount = params.containsKey("user_account") ? (String) params.get("user_account") : null;
            return queryResources(type, userAccount);
        });
        
        return functions;
    }
}