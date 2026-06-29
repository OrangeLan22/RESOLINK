package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.SpaceResource;
import com.orangelan.resolinkserver.entity.PhysicalResource;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.entity.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 资源管理控制器
 * 用于处理资源相关的API请求
 */
@RestController
@RequestMapping("/api/resource-mgm")
public class ResourceMgmController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceMgmController.class);
    private final SpaceResourceRepository spaceResourceRepository;
    private final PhysicalResourceRepository physicalResourceRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    
    public ResourceMgmController(SpaceResourceRepository spaceResourceRepository, PhysicalResourceRepository physicalResourceRepository, DepartmentRepository departmentRepository, AppointmentRepository appointmentRepository) {
        this.spaceResourceRepository = spaceResourceRepository;
        this.physicalResourceRepository = physicalResourceRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }
    
    /**
     * 获取所有资源API
     * @return 资源列表
     */
    @GetMapping("/get-resources")
    public ResponseEntity<Map<String, Object>> getResources() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            List<SpaceResource> resources = spaceResourceRepository.findAll();
            
            logger.info("获取资源列表成功，共找到{}条记录", resources.size());
            
            response.put("success", true);
            response.put("message", "获取资源列表成功");
            response.put("data", resources);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取资源列表失败：{}", e.getMessage());
            response.put("success", false);
            response.put("message", "获取资源列表失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 创建资源API
     * @param request 请求体，包含创建资源所需的数据
     * @return 创建结果
     */
    @PostMapping("/create-resource")
    public ResponseEntity<Map<String, Object>> createResource(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("创建资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查data字段的类型，支持单个对象或数组
            Object dataObj = request.get("data");
            
            List<SpaceResource> savedResources = new ArrayList<>();
            
            if (dataObj instanceof Map) {
                // 处理单个资源
                Map<String, Object> data = (Map<String, Object>) dataObj;
                SpaceResource savedResource = processResourceData(data);
                savedResources.add(savedResource);
            } else if (dataObj instanceof List) {
                // 处理批量资源
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                for (Map<String, Object> data : dataList) {
                    SpaceResource savedResource = processResourceData(data);
                    savedResources.add(savedResource);
                }
            } else {
                logger.error("创建资源失败：data字段类型错误");
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("资源创建成功，共创建{}个资源", savedResources.size());
            
            response.put("success", true);
            response.put("message", "资源创建成功");
            response.put("data", savedResources.size() == 1 ? savedResources.get(0) : savedResources);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("创建资源失败：参数错误", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (ClassCastException e) {
            logger.error("创建资源失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("创建资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "资源创建失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 处理单个资源数据
     * @param data 资源数据
     * @return 保存后的资源
     */
    private SpaceResource processResourceData(Map<String, Object> data) {
        // 验证必要字段是否存在
        if (!data.containsKey("space_name") || !data.containsKey("location") || !data.containsKey("type") || 
            !data.containsKey("capacity") || !data.containsKey("public") || !data.containsKey("check") || !data.containsKey("note")) {
            logger.error("创建资源失败：缺少必要字段");
            throw new IllegalArgumentException("数据不完整");
        }
        
        // 获取并验证字段值
        String spaceName = data.get("space_name") instanceof String ? (String) data.get("space_name") : null;
        String location = data.get("location") instanceof String ? (String) data.get("location") : null;
        String type = data.get("type") instanceof String ? (String) data.get("type") : null;
        Integer capacity = data.get("capacity") instanceof Number ? ((Number) data.get("capacity")).intValue() : null;
        Integer publicFlag = data.get("public") instanceof Number ? ((Number) data.get("public")).intValue() : null;
        Integer checkFlag = data.get("check") instanceof Number ? ((Number) data.get("check")).intValue() : null;
        String note = data.get("note") instanceof String ? (String) data.get("note") : null;
        
        // 处理部门ID（支持逗号分隔的多个部门ID）
        String depId = null;
        Object depIdObj = data.get("dep_id");
        if (depIdObj != null) {
            depId = depIdObj.toString();
        }
        
        // 获取tag字段（非必填，默认为空字符串）
        String tag = data.get("tag") instanceof String ? (String) data.get("tag") : "";
        
        // 验证必要字段是否存在且非空
        if (spaceName == null || spaceName.trim().isEmpty() || location == null || location.trim().isEmpty() || 
            type == null || type.trim().isEmpty() || capacity == null || publicFlag == null || 
            checkFlag == null || note == null) {
            logger.error("创建资源失败：字段值为空或无效");
            throw new IllegalArgumentException("数据不完整");
        }
        
        // 验证capacity的范围（1~1000）
        if (capacity < 1 || capacity > 1000) {
            logger.error("创建资源失败：capacity值必须在1~1000之间");
            throw new IllegalArgumentException("capacity值非法");
        }
        
        // 验证publicFlag的值只能是0或1
        if (publicFlag != 0 && publicFlag != 1) {
            logger.error("创建资源失败：public值只能是0或1");
            throw new IllegalArgumentException("public值非法");
        }
        
        // 根据publicFlag验证dep_id
        if (publicFlag == 0) {
            // 如果public为0，必须提供dep_id
            if (depId == null || depId.trim().isEmpty()) {
                logger.error("创建资源失败：public为0时必须提供dep_id");
                throw new IllegalArgumentException("数据不完整");
            }
            
            // 解析逗号分隔的部门ID
            String[] depIdArray = depId.split(",");
            for (String idStr : depIdArray) {
                idStr = idStr.trim();
                try {
                    Long departmentId = Long.parseLong(idStr);
                    // 检查部门是否存在
                    Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
                    if (!departmentOptional.isPresent()) {
                        logger.error("创建资源失败：部门不存在（ID：{}", departmentId);
                        throw new IllegalArgumentException("非法的数据");
                    }
                } catch (NumberFormatException e) {
                    logger.error("创建资源失败：部门ID格式错误（ID：{}", idStr);
                    throw new IllegalArgumentException("非法的数据");
                }
            }
        } else {
            // 如果public为1，忽略dep_id
            depId = null;
        }
        
        // 创建SpaceResource实体
        SpaceResource spaceResource = new SpaceResource();
        spaceResource.setSpaceName(spaceName);
        spaceResource.setLocation(location);
        spaceResource.setType(type);
        spaceResource.setCapacity(capacity);
        spaceResource.setPublicFlag(publicFlag);
        spaceResource.setDepId(depId);
        spaceResource.setCheckFlag(checkFlag);
        spaceResource.setNote(note);
        spaceResource.setTag(tag);
        
        // 保存到数据库
        SpaceResource savedResource = spaceResourceRepository.save(spaceResource);
        
        logger.info("资源创建成功：ID：{}，资源名称：{}", savedResource.getId(), savedResource.getSpaceName());
        
        return savedResource;
    }
    
    /**
     * 根据ID获取资源API
     * @param id 资源ID
     * @return 资源信息
     */
    @GetMapping("/get-resource/{id}")
    public ResponseEntity<Map<String, Object>> getResourceById(@PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            Optional<SpaceResource> resourceOptional = spaceResourceRepository.findById(id);
            
            if (!resourceOptional.isPresent()) {
                logger.error("获取资源失败：资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            SpaceResource resource = resourceOptional.get();
            
            logger.info("获取资源成功：ID：{}，资源名称：{}", resource.getId(), resource.getSpaceName());
            
            response.put("success", true);
            response.put("message", "获取资源成功");
            response.put("data", resource);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取资源失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 更新资源API
     * @param request 请求体，包含要更新的资源信息
     * @return 更新结果
     */
    @PostMapping("/update-resource")
    public ResponseEntity<Map<String, Object>> updateResource(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("更新资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("更新资源失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段值
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            }
            
            if (id == null) {
                logger.error("更新资源失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询资源
            Optional<SpaceResource> resourceOptional = spaceResourceRepository.findById(id);
            if (!resourceOptional.isPresent()) {
                logger.error("更新资源失败：资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            SpaceResource spaceResource = resourceOptional.get();
            
            // 验证必要字段是否存在
            if (!data.containsKey("space_name") || !data.containsKey("location") || !data.containsKey("type") || 
                !data.containsKey("capacity") || !data.containsKey("public") || !data.containsKey("check") || !data.containsKey("note")) {
                logger.error("更新资源失败：缺少必要字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证字段值
            String spaceName = data.get("space_name") instanceof String ? (String) data.get("space_name") : null;
            String location = data.get("location") instanceof String ? (String) data.get("location") : null;
            String type = data.get("type") instanceof String ? (String) data.get("type") : null;
            Integer capacity = data.get("capacity") instanceof Number ? ((Number) data.get("capacity")).intValue() : null;
            Integer publicFlag = data.get("public") instanceof Number ? ((Number) data.get("public")).intValue() : null;
            Integer checkFlag = data.get("check") instanceof Number ? ((Number) data.get("check")).intValue() : null;
            String note = data.get("note") instanceof String ? (String) data.get("note") : null;
            
            // 处理部门ID（支持逗号分隔的多个部门ID）
            String depId = null;
            if (data.containsKey("dep_id")) {
                Object depIdObj = data.get("dep_id");
                if (depIdObj != null) {
                    depId = depIdObj.toString();
                }
            }
            
            // 验证必要字段是否存在且非空
            if (spaceName == null || spaceName.trim().isEmpty() || location == null || location.trim().isEmpty() || 
                type == null || type.trim().isEmpty() || capacity == null || publicFlag == null || 
                checkFlag == null || note == null) {
                logger.error("更新资源失败：字段值为空或无效");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证capacity的范围（1~1000）
            if (capacity < 1 || capacity > 1000) {
                logger.error("更新资源失败：capacity值必须在1~1000之间");
                response.put("success", false);
                response.put("message", "capacity值非法");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证publicFlag的值只能是0或1
            if (publicFlag != 0 && publicFlag != 1) {
                logger.error("更新资源失败：public值只能是0或1");
                response.put("success", false);
                response.put("message", "public值非法");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据publicFlag验证dep_id
            if (publicFlag == 0) {
                // 如果public为0，必须提供dep_id
                if (depId == null || depId.trim().isEmpty()) {
                    logger.error("更新资源失败：public为0时必须提供dep_id");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 解析逗号分隔的部门ID
                String[] depIdArray = depId.split(",");
                for (String idStr : depIdArray) {
                    idStr = idStr.trim();
                    try {
                        Long departmentId = Long.parseLong(idStr);
                        // 检查部门是否存在
                        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
                        if (!departmentOptional.isPresent()) {
                            logger.error("更新资源失败：部门不存在（ID：{}", departmentId);
                            response.put("success", false);
                            response.put("message", "非法的数据");
                            return ResponseEntity.badRequest().body(response);
                        }
                    } catch (NumberFormatException e) {
                        logger.error("更新资源失败：部门ID格式错误（ID：{}", idStr);
                        response.put("success", false);
                        response.put("message", "非法的数据");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            } else {
                // 如果public为1，忽略dep_id
                depId = null;
            }
            
            // 更新资源信息
            spaceResource.setSpaceName(spaceName);
            spaceResource.setLocation(location);
            spaceResource.setType(type);
            spaceResource.setCapacity(capacity);
            spaceResource.setPublicFlag(publicFlag);
            spaceResource.setDepId(depId);
            spaceResource.setCheckFlag(checkFlag);
            spaceResource.setNote(note);
            
            // 保存到数据库
            SpaceResource updatedResource = spaceResourceRepository.save(spaceResource);
            
            logger.info("资源更新成功：ID：{}，资源名称：{}", updatedResource.getId(), updatedResource.getSpaceName());
            
            response.put("success", true);
            response.put("message", "资源更新成功");
            response.put("data", updatedResource);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("更新资源失败：id不是有效的数字格式", e);
            response.put("success", false);
            response.put("message", "数据格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (ClassCastException e) {
            logger.error("更新资源失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("更新资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "资源更新失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有设备资源API
     * @return 设备资源列表
     */
    @GetMapping("/get-physicals")
    public ResponseEntity<Map<String, Object>> getPhysicals() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            List<PhysicalResource> resources = physicalResourceRepository.findAll();
            
            logger.info("获取设备资源列表成功，共找到{}条记录", resources.size());
            
            response.put("success", true);
            response.put("message", "获取设备资源列表成功");
            response.put("data", resources);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取设备资源列表失败：{}", e.getMessage());
            response.put("success", false);
            response.put("message", "获取设备资源列表失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 创建设备资源API
     * @param request 请求体，包含创建设备资源所需的数据
     * @return 创建结果
     */
    @PostMapping("/create-physical")
    public ResponseEntity<Map<String, Object>> createPhysical(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("创建设备资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查data字段的类型，支持单个对象或数组
            Object dataObj = request.get("data");
            
            List<PhysicalResource> savedResources = new ArrayList<>();
            
            if (dataObj instanceof Map) {
                // 处理单个资源
                Map<String, Object> data = (Map<String, Object>) dataObj;
                PhysicalResource savedResource = processPhysicalData(data);
                savedResources.add(savedResource);
            } else if (dataObj instanceof List) {
                // 处理批量资源
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                for (Map<String, Object> data : dataList) {
                    PhysicalResource savedResource = processPhysicalData(data);
                    savedResources.add(savedResource);
                }
            } else {
                logger.error("创建设备资源失败：data字段类型错误");
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("设备资源创建成功，共创建{}个资源", savedResources.size());
            
            response.put("success", true);
            response.put("message", "设备资源创建成功");
            response.put("data", savedResources.size() == 1 ? savedResources.get(0) : savedResources);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("创建设备资源失败：参数错误", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (ClassCastException e) {
            logger.error("创建设备资源失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("创建设备资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "设备资源创建失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据ID获取设备资源API
     * @param id 设备资源ID
     * @return 设备资源信息
     */
    @GetMapping("/get-physical/{id}")
    public ResponseEntity<Map<String, Object>> getPhysicalById(@PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            Optional<PhysicalResource> resourceOptional = physicalResourceRepository.findById(id);
            
            if (!resourceOptional.isPresent()) {
                logger.error("获取设备资源失败：设备资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "设备资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            PhysicalResource resource = resourceOptional.get();
            
            logger.info("获取设备资源成功：ID：{}，设备名称：{}", resource.getId(), resource.getEquipmentName());
            
            response.put("success", true);
            response.put("message", "获取设备资源成功");
            response.put("data", resource);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取设备资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取设备资源失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 更新设备资源API
     * @param request 请求体，包含要更新的设备资源信息
     * @return 更新结果
     */
    @PostMapping("/update-physical")
    public ResponseEntity<Map<String, Object>> updatePhysical(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("更新设备资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("更新设备资源失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段值
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            }
            
            if (id == null) {
                logger.error("更新设备资源失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询设备资源
            Optional<PhysicalResource> resourceOptional = physicalResourceRepository.findById(id);
            if (!resourceOptional.isPresent()) {
                logger.error("更新设备资源失败：设备资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "设备资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            PhysicalResource physicalResource = resourceOptional.get();
            
            // 验证必要字段是否存在
            if (!data.containsKey("equipment_name") || !data.containsKey("location") || !data.containsKey("type") || 
                !data.containsKey("public") || !data.containsKey("check") || !data.containsKey("note")) {
                logger.error("更新设备资源失败：缺少必要字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证字段值
            String equipmentName = data.get("equipment_name") instanceof String ? (String) data.get("equipment_name") : null;
            String location = data.get("location") instanceof String ? (String) data.get("location") : null;
            String type = data.get("type") instanceof String ? (String) data.get("type") : null;
            Integer publicFlag = data.get("public") instanceof Number ? ((Number) data.get("public")).intValue() : null;
            Integer checkFlag = data.get("check") instanceof Number ? ((Number) data.get("check")).intValue() : null;
            String note = data.get("note") instanceof String ? (String) data.get("note") : null;
            
            // 处理部门ID（支持逗号分隔的多个部门ID）
            String depId = null;
            if (data.containsKey("dep_id")) {
                Object depIdObj = data.get("dep_id");
                if (depIdObj != null) {
                    depId = depIdObj.toString();
                }
            }
            
            // 验证必要字段是否存在且非空
            if (equipmentName == null || equipmentName.trim().isEmpty() || location == null || location.trim().isEmpty() || 
                type == null || type.trim().isEmpty() || publicFlag == null || 
                checkFlag == null || note == null) {
                logger.error("更新设备资源失败：字段值为空或无效");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证publicFlag的值只能是0或1
            if (publicFlag != 0 && publicFlag != 1) {
                logger.error("更新设备资源失败：public值只能是0或1");
                response.put("success", false);
                response.put("message", "public值非法");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据publicFlag验证dep_id
            if (publicFlag == 0) {
                // 如果public为0，必须提供dep_id
                if (depId == null || depId.trim().isEmpty()) {
                    logger.error("更新设备资源失败：public为0时必须提供dep_id");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 解析逗号分隔的部门ID
                String[] depIdArray = depId.split(",");
                for (String idStr : depIdArray) {
                    idStr = idStr.trim();
                    try {
                        Long departmentId = Long.parseLong(idStr);
                        // 检查部门是否存在
                        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
                        if (!departmentOptional.isPresent()) {
                            logger.error("更新设备资源失败：部门不存在（ID：{}", departmentId);
                            response.put("success", false);
                            response.put("message", "非法的数据");
                            return ResponseEntity.badRequest().body(response);
                        }
                    } catch (NumberFormatException e) {
                        logger.error("更新设备资源失败：部门ID格式错误（ID：{}", idStr);
                        response.put("success", false);
                        response.put("message", "非法的数据");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            } else {
                // 如果public为1，忽略dep_id
                depId = null;
            }
            
            // 更新设备资源信息
            physicalResource.setEquipmentName(equipmentName);
            physicalResource.setLocation(location);
            physicalResource.setType(type);
            physicalResource.setPublicFlag(publicFlag);
            physicalResource.setDepId(depId);
            physicalResource.setCheckFlag(checkFlag);
            physicalResource.setNote(note);
            
            // 保存到数据库
            PhysicalResource updatedResource = physicalResourceRepository.save(physicalResource);
            
            logger.info("设备资源更新成功：ID：{}，设备名称：{}", updatedResource.getId(), updatedResource.getEquipmentName());
            
            response.put("success", true);
            response.put("message", "设备资源更新成功");
            response.put("data", updatedResource);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("更新设备资源失败：id不是有效的数字格式", e);
            response.put("success", false);
            response.put("message", "数据格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (ClassCastException e) {
            logger.error("更新设备资源失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("更新设备资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "设备资源更新失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除设备资源API
     * @param request 请求体，包含要删除的设备资源ID
     * @return 删除结果
     */
    @PostMapping("/delete-physical")
    public ResponseEntity<Map<String, Object>> deletePhysical(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("删除设备资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("删除设备资源失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段值
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            }
            
            if (id == null) {
                logger.error("删除设备资源失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询设备资源
            Optional<PhysicalResource> resourceOptional = physicalResourceRepository.findById(id);
            if (!resourceOptional.isPresent()) {
                logger.error("删除设备资源失败：设备资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "设备资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            PhysicalResource resource = resourceOptional.get();
            
            // 删除设备资源
            physicalResourceRepository.delete(resource);
            
            logger.info("设备资源删除成功：ID：{}，设备名称：{}", id, resource.getEquipmentName());
            
            response.put("success", true);
            response.put("message", "设备资源删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("删除设备资源失败：id不是有效的数字格式", e);
            response.put("success", false);
            response.put("message", "数据格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("删除设备资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "设备资源删除失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 处理单个设备资源数据
     * @param data 设备资源数据
     * @return 保存后的设备资源
     */
    private PhysicalResource processPhysicalData(Map<String, Object> data) {
        // 验证必要字段是否存在
        if (!data.containsKey("equipment_name") || !data.containsKey("location") || !data.containsKey("type") || 
            !data.containsKey("public") || !data.containsKey("check") || !data.containsKey("note")) {
            logger.error("创建设备资源失败：缺少必要字段");
            throw new IllegalArgumentException("数据不完整");
        }
        
        // 获取并验证字段值
        String equipmentName = data.get("equipment_name") instanceof String ? (String) data.get("equipment_name") : null;
        String location = data.get("location") instanceof String ? (String) data.get("location") : null;
        String type = data.get("type") instanceof String ? (String) data.get("type") : null;
        Integer publicFlag = data.get("public") instanceof Number ? ((Number) data.get("public")).intValue() : null;
        Integer checkFlag = data.get("check") instanceof Number ? ((Number) data.get("check")).intValue() : null;
        String note = data.get("note") instanceof String ? (String) data.get("note") : null;
        
        // 处理部门ID（支持逗号分隔的多个部门ID）
        String depId = null;
        if (data.containsKey("dep_id")) {
            Object depIdObj = data.get("dep_id");
            if (depIdObj != null) {
                depId = depIdObj.toString();
            }
        }
        
        // 获取tag字段（非必填，默认为空字符串）
        String tag = data.get("tag") instanceof String ? (String) data.get("tag") : "";
        
        // 验证必要字段是否存在且非空
        if (equipmentName == null || equipmentName.trim().isEmpty() || location == null || location.trim().isEmpty() || 
            type == null || type.trim().isEmpty() || publicFlag == null || 
            checkFlag == null || note == null) {
            logger.error("创建设备资源失败：字段值为空或无效");
            throw new IllegalArgumentException("数据不完整");
        }
        
        // 验证publicFlag的值只能是0或1
        if (publicFlag != 0 && publicFlag != 1) {
            logger.error("创建设备资源失败：public值只能是0或1");
            throw new IllegalArgumentException("public值非法");
        }
        
        // 根据publicFlag验证dep_id
        if (publicFlag == 0) {
            // 如果public为0，必须提供dep_id
            if (depId == null || depId.trim().isEmpty()) {
                logger.error("创建设备资源失败：public为0时必须提供dep_id");
                throw new IllegalArgumentException("数据不完整");
            }
            
            // 解析逗号分隔的部门ID
            String[] depIdArray = depId.split(",");
            for (String idStr : depIdArray) {
                idStr = idStr.trim();
                try {
                    Long departmentId = Long.parseLong(idStr);
                    // 检查部门是否存在
                    Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
                    if (!departmentOptional.isPresent()) {
                        logger.error("创建设备资源失败：部门不存在（ID：{}", departmentId);
                        throw new IllegalArgumentException("非法的数据");
                    }
                } catch (NumberFormatException e) {
                    logger.error("创建设备资源失败：部门ID格式错误（ID：{}", idStr);
                    throw new IllegalArgumentException("非法的数据");
                }
            }
        } else {
            // 如果public为1，忽略dep_id
            depId = null;
        }
        
        // 创建PhysicalResource实体
        PhysicalResource physicalResource = new PhysicalResource();
        physicalResource.setEquipmentName(equipmentName);
        physicalResource.setLocation(location);
        physicalResource.setType(type);
        physicalResource.setPublicFlag(publicFlag);
        physicalResource.setDepId(depId);
        physicalResource.setCheckFlag(checkFlag);
        physicalResource.setNote(note);
        physicalResource.setTag(tag);
        
        // 保存到数据库
        PhysicalResource savedResource = physicalResourceRepository.save(physicalResource);
        
        logger.info("设备资源创建成功：ID：{}，设备名称：{}", savedResource.getId(), savedResource.getEquipmentName());
        
        return savedResource;
    }
    
    /**
     * 删除资源API
     * @param request 请求体，包含要删除的资源ID
     * @return 删除结果
     */
    @PostMapping("/delete-resource")
    public ResponseEntity<Map<String, Object>> deleteResource(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null || !request.containsKey("data")) {
                logger.error("删除资源失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("删除资源失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证id字段值
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            }
            
            if (id == null) {
                logger.error("删除资源失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询资源
            Optional<SpaceResource> resourceOptional = spaceResourceRepository.findById(id);
            if (!resourceOptional.isPresent()) {
                logger.error("删除资源失败：资源不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "资源不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            SpaceResource resource = resourceOptional.get();
            
            // 删除资源
            spaceResourceRepository.delete(resource);
            
            logger.info("资源删除成功：ID：{}，资源名称：{}", id, resource.getSpaceName());
            
            response.put("success", true);
            response.put("message", "资源删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("删除资源失败：id不是有效的数字格式", e);
            response.put("success", false);
            response.put("message", "数据格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("删除资源失败：系统错误", e);
            response.put("success", false);
            response.put("message", "资源删除失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取待审批预约列表API
     * @return 待审批预约列表
     */
    @GetMapping("/get-pending-approvals")
    public ResponseEntity<Map<String, Object>> getPendingApprovals() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            List<Appointment> pendingAppointments = appointmentRepository.findByApproval(0);
            
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Appointment appointment : pendingAppointments) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", appointment.getId());
                item.put("emp_id", appointment.getEmpId());
                item.put("name", appointment.getName());
                item.put("res_name", appointment.getResName());
                item.put("start_time", appointment.getStartTime());
                item.put("end_time", appointment.getEndTime());
                item.put("appointment_date", appointment.getAppointmentDate());
                
                String location = "";
                if ("space".equals(appointment.getType())) {
                    SpaceResource spaceResource = spaceResourceRepository.findById(appointment.getResId()).orElse(null);
                    if (spaceResource != null) {
                        location = spaceResource.getLocation();
                    }
                } else if ("physical".equals(appointment.getType())) {
                    PhysicalResource physicalResource = physicalResourceRepository.findById(appointment.getResId()).orElse(null);
                    if (physicalResource != null) {
                        location = physicalResource.getLocation();
                    }
                }
                item.put("location", location);
                
                resultList.add(item);
            }
            
            logger.info("获取待审批预约列表成功，共找到{}条记录", resultList.size());
            
            response.put("success", true);
            response.put("message", "获取待审批预约列表成功");
            response.put("data", resultList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取待审批预约列表失败：{}", e.getMessage());
            response.put("success", false);
            response.put("message", "获取待审批预约列表失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 预约审批API
     * @param request 请求体，包含id和boolean字段
     * @return 审批结果
     */
    @PostMapping("/approve-appointment")
    public ResponseEntity<Map<String, Object>> approveAppointment(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            if (request == null || !request.containsKey("data")) {
                logger.error("预约审批失败：请求体为空或缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            if (!data.containsKey("id")) {
                logger.error("预约审批失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("boolean")) {
                logger.error("预约审批失败：data字段缺少boolean");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long id = null;
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            }
            
            if (id == null) {
                logger.error("预约审批失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            Boolean approvalFlag = null;
            Object booleanObj = data.get("boolean");
            if (booleanObj instanceof Boolean) {
                approvalFlag = (Boolean) booleanObj;
            } else if (booleanObj instanceof String) {
                approvalFlag = Boolean.parseBoolean((String) booleanObj);
            }
            
            if (approvalFlag == null) {
                logger.error("预约审批失败：boolean字段无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);
            if (!appointmentOptional.isPresent()) {
                logger.error("预约审批失败：预约记录不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "预约记录不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            if (approvalFlag) {
                appointment.setApproval(1);
            } else {
                appointment.setApproval(-1);
            }
            
            appointmentRepository.save(appointment);
            
            logger.info("预约审批成功：ID：{}，审批结果：{}", id, approvalFlag ? "通过" : "拒绝");
            
            response.put("success", true);
            response.put("message", "预约审批成功");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("预约审批失败：id不是有效的数字格式", e);
            response.put("success", false);
            response.put("message", "数据格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("预约审批失败：系统错误", e);
            response.put("success", false);
            response.put("message", "预约审批失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}