package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Authority;
import com.orangelan.resolinkserver.entity.Department;
import com.orangelan.resolinkserver.entity.Public;
import com.orangelan.resolinkserver.entity.Status;
import com.orangelan.resolinkserver.repository.AuthorityRepository;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.repository.PublicRepository;
import com.orangelan.resolinkserver.repository.StatusRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public-info")
public class PublicInfoController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicInfoController.class);
    private final PublicRepository publicRepository;
    private final DepartmentRepository departmentRepository;
    private final StatusRepository statusRepository;
    private final AuthorityRepository authorityRepository;
    private final UserInfoRepository userInfoRepository;
    private final PhysicalResourceRepository physicalResourceRepository;
    private final SpaceResourceRepository spaceResourceRepository;
    private final JwtTokenUtil jwtTokenUtil;
    
    public PublicInfoController(PublicRepository publicRepository, DepartmentRepository departmentRepository, StatusRepository statusRepository, AuthorityRepository authorityRepository, UserInfoRepository userInfoRepository, PhysicalResourceRepository physicalResourceRepository, SpaceResourceRepository spaceResourceRepository, JwtTokenUtil jwtTokenUtil) {
        this.publicRepository = publicRepository;
        this.departmentRepository = departmentRepository;
        this.statusRepository = statusRepository;
        this.authorityRepository = authorityRepository;
        this.userInfoRepository = userInfoRepository;
        this.physicalResourceRepository = physicalResourceRepository;
        this.spaceResourceRepository = spaceResourceRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 查询所有部门和对应的身份信息，构造响应数据格式
     * @return 包含部门和身份信息的列表
     */
    private List<Map<String, Object>> getDepartmentsWithStatusesList() {
        // 查询所有部门
        List<Department> departments = departmentRepository.findAll();
        
        // 构造响应数据
        List<Map<String, Object>> departmentsWithStatuses = new ArrayList<>();
        for (Department department : departments) {
            // 创建部门信息映射（使用LinkedHashMap保证顺序）
            Map<String, Object> departmentMap = new LinkedHashMap<>();
            departmentMap.put("dep_id", department.getId());
            departmentMap.put("dep_name", department.getDepName());
            
            // 查询该部门下的所有身份
            List<Status> statuses = statusRepository.findAll().stream()
                    .filter(status -> status.getDepartment().getId().equals(department.getId()))
                    .toList();
            
            // 提取身份信息列表（包含id和名称，使用LinkedHashMap保证顺序）
            List<Map<String, Object>> statusList = statuses.stream()
                    .map(status -> {
                        Map<String, Object> statusMap = new LinkedHashMap<>();
                        statusMap.put("sta_id", status.getId());
                        statusMap.put("sta_name", status.getStaName());
                        return statusMap;
                    })
                    .toList();
            
            // 将身份列表添加到部门信息中
            departmentMap.put("statuses", statusList);
            
            // 将部门信息添加到响应数据中
            departmentsWithStatuses.add(departmentMap);
        }
        
        return departmentsWithStatuses;
    }
    
    /**
     * 修改企业名称
     * @param request 包含新企业名称的请求体
     * @return 修改结果
     */
    @PostMapping("/company-name")
    public ResponseEntity<Map<String, Object>> updateCompanyName(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求参数
            if (request == null || request.get("company-name") == null || request.get("company-name").trim().isEmpty()) {
                logger.error("修改企业名称失败：企业名称不能为空");
                response.put("success", false);
                response.put("message", "企业名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取新的企业名称
            String newCompanyName = request.get("company-name").trim();
            logger.info("新的企业名称：{}", newCompanyName);
            
            // 查询public表
            List<Public> publicList = publicRepository.findAll();
            if (publicList.isEmpty()) {
                // 如果public表为空，创建新记录
                Public publicInfo = new Public();
                publicInfo.setCompanyName(newCompanyName);
                publicRepository.save(publicInfo);
                logger.info("成功创建公共信息，企业名称：{}", newCompanyName);
            } else {
                // 如果public表不为空，更新第一条记录
                Public publicInfo = publicList.get(0);
                publicInfo.setCompanyName(newCompanyName);
                publicRepository.save(publicInfo);
                logger.info("成功更新企业名称为：{}", newCompanyName);
            }
            
            // 返回成功响应
            response.put("success", true);
            response.put("message", "企业名称修改成功");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("修改企业名称失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "修改企业名称失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("修改企业名称失败：系统错误", e);
            response.put("success", false);
            response.put("message", "修改企业名称失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有权限信息
     * @return 权限信息列表
     */
    @GetMapping("/authorities")
    public ResponseEntity<Map<String, Object>> getAllAuthorities() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 查询所有权限信息
            List<Authority> authorities = authorityRepository.findAll();
            
            // 构造响应数据格式
            List<Map<String, Object>> authorityList = new ArrayList<>();
            for (Authority authority : authorities) {
                Map<String, Object> authorityMap = new LinkedHashMap<>();
                authorityMap.put("id", authority.getId());
                authorityMap.put("tag", authority.getTag());
                
                // 构造auth字段
                Map<String, Integer> authMap = new LinkedHashMap<>();
                authMap.put("appointment", authority.getAppointment());
                authMap.put("public-info", authority.getPublicInfo());
                authMap.put("account-mgm", authority.getAccountMgm());
                authMap.put("resource-mgm", authority.getResourceMgm());
                authMap.put("history", authority.getHistory());
                authMap.put("check", authority.getCheck());
                
                // 将authMap添加到列表中
                List<Map<String, Integer>> authList = new ArrayList<>();
                authList.add(authMap);
                
                authorityMap.put("auth", authList);
                authorityList.add(authorityMap);
            }
            
            // 返回成功响应
            response.put("success", true);
            response.put("message", "获取权限信息成功");
            response.put("data", authorityList);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("获取权限信息失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取权限信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取权限信息失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取权限信息失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 新增、修改、删除权限
     * @param request 请求体，包含权限信息
     * @return 处理结果
     */
    @PostMapping("/manage-authorities")
    public ResponseEntity<Map<String, Object>> manageAuthorities(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求参数格式
            if (request == null || request.get("data") == null) {
                logger.error("处理权限信息失败：请求参数不完整，缺少data字段");
                response.put("success", false);
                response.put("message", "请求参数不完整，缺少data字段");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 解析data数组
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) request.get("data");
            if (dataList == null || dataList.isEmpty()) {
                logger.error("处理权限信息失败：data数组为空");
                response.put("success", false);
                response.put("message", "data数组为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 遍历data数组，处理每个权限对象
            for (Map<String, Object> authorityData : dataList) {
                try {
                    // 验证基础参数（id和tag是所有操作都需要的）
                    if (authorityData == null || authorityData.get("id") == null || authorityData.get("tag") == null) {
                        logger.info("跳过处理权限：请求参数不完整，缺少id或tag");
                        continue; // 参数不完整，跳过处理
                    }
                    
                    // 解析基础参数
                    int id = Integer.parseInt(authorityData.get("id").toString());
                    String tag = (String) authorityData.get("tag");
                    
                    // 检查是否为超级管理员权限（id=99或tag="高级管理员"）
                    if (id == 99 || "高级管理员".equals(tag)) {
                        logger.info("跳过操作：高级管理员权限禁止修改、删除、增加");
                        continue; // 高级管理员权限，跳过处理
                    }
                    
                    // 解析auth参数（仅新增和修改操作需要）
                    int appointment = 0;
                    int publicInfo = 0;
                    int accountMgm = 0;
                    int resourceMgm = 0;
                    int history = 0;
                    int check = 0;
                    
                    // 只有新增和修改操作需要验证和解析auth参数
                    if (id != -1) {
                        if (authorityData.get("auth") == null) {
                            logger.info("跳过处理权限：请求参数不完整，缺少auth字段");
                            continue; // auth参数不完整，跳过处理
                        }
                        
                        List<Map<String, Object>> authList = (List<Map<String, Object>>) authorityData.get("auth");
                        if (authList == null || authList.isEmpty()) {
                            logger.info("跳过处理权限：auth参数不完整");
                            continue; // auth参数不完整，跳过处理
                        }
                        
                        Map<String, Object> authMap = authList.get(0);
                        appointment = Integer.parseInt(authMap.getOrDefault("appointment", 0).toString());
                        publicInfo = Integer.parseInt(authMap.getOrDefault("public-info", 0).toString());
                        accountMgm = Integer.parseInt(authMap.getOrDefault("account-mgm", 0).toString());
                        resourceMgm = Integer.parseInt(authMap.getOrDefault("resource-mgm", 0).toString());
                        history = Integer.parseInt(authMap.getOrDefault("history", 0).toString());
                        check = Integer.parseInt(authMap.getOrDefault("check", 0).toString());
                    }
                    
                    if (id == 0) {
                        // 新增权限
                        // 检查是否已存在同名权限
                        Authority existingAuthority = authorityRepository.findByTag(tag);
                        if (existingAuthority == null) {
                            // 权限不存在，执行添加
                            Authority newAuthority = new Authority();
                            newAuthority.setTag(tag);
                            newAuthority.setAppointment(appointment);
                            newAuthority.setPublicInfo(publicInfo);
                            newAuthority.setAccountMgm(accountMgm);
                            newAuthority.setResourceMgm(resourceMgm);
                            newAuthority.setHistory(history);
                            newAuthority.setCheck(check);
                            authorityRepository.save(newAuthority);
                            logger.info("成功添加权限：{}", tag);
                        } else {
                            // 权限已存在，跳过添加
                            logger.info("跳过添加权限：{}- 已存在", tag);
                        }
                    } else if (id == -1) {
                        // 删除权限
                        Authority authorityToDelete = authorityRepository.findByTag(tag);
                        if (authorityToDelete != null) {
                            // 检查user_info表中是否有使用该权限的记录
                            boolean existsInUserInfo = userInfoRepository.existsByAuthId(authorityToDelete.getId());
                            if (existsInUserInfo) {
                                // 存在未删除的绑定信息，返回错误
                                logger.error("删除权限失败：存在未删除的绑定信息（权限：{}", tag);
                                response.put("success", false);
                                response.put("message", "存在未删除的绑定信息");
                                return ResponseEntity.badRequest().body(response);
                            }
                            // 权限存在且无绑定信息，执行删除
                            authorityRepository.delete(authorityToDelete);
                            logger.info("成功删除权限：{}", tag);
                        } else {
                            // 权限不存在，跳过删除
                            logger.info("未找到要删除的权限：{}", tag);
                        }
                    } else {
                        // 修改权限
                        Authority authorityToUpdate = authorityRepository.findById((long) id).orElse(null);
                        if (authorityToUpdate != null) {
                            // 检查新tag是否与其他权限冲突
                            Authority existingAuthority = authorityRepository.findByTag(tag);
                            if (existingAuthority == null || existingAuthority.getId() == id) {
                                // 新tag可用（不存在或就是当前权限的tag），执行修改
                                authorityToUpdate.setTag(tag);
                                authorityToUpdate.setAppointment(appointment);
                                authorityToUpdate.setPublicInfo(publicInfo);
                                authorityToUpdate.setAccountMgm(accountMgm);
                                authorityToUpdate.setResourceMgm(resourceMgm);
                                authorityToUpdate.setHistory(history);
                                authorityToUpdate.setCheck(check);
                                authorityRepository.save(authorityToUpdate);
                                logger.info("成功修改权限：{}（ID：{}）", tag, id);
                            } else {
                                // 新tag已被其他权限使用，跳过修改
                                logger.info("跳过修改权限：{}- 权限名称已被使用", tag);
                            }
                        } else {
                            // 权限不存在，跳过修改
                            logger.info("未找到要修改的权限（ID：{}）", id);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.info("跳过处理权限：数字格式错误 - {}", e.getMessage());
                    continue; // 数字格式错误，跳过处理
                } catch (Exception e) {
                    logger.info("跳过处理权限：处理异常 - {}", e.getMessage());
                    continue; // 其他异常，跳过处理
                }
            }
            
            // 查询所有权限信息并返回
            List<Authority> authorities = authorityRepository.findAll();
            List<Map<String, Object>> authorityList = new ArrayList<>();
            for (Authority authority : authorities) {
                Map<String, Object> authorityMap = new LinkedHashMap<>();
                authorityMap.put("id", authority.getId());
                authorityMap.put("tag", authority.getTag());
                
                Map<String, Integer> authMapResponse = new LinkedHashMap<>();
                authMapResponse.put("appointment", authority.getAppointment());
                authMapResponse.put("public-info", authority.getPublicInfo());
                authMapResponse.put("account-mgm", authority.getAccountMgm());
                authMapResponse.put("resource-mgm", authority.getResourceMgm());
                authMapResponse.put("history", authority.getHistory());
                authMapResponse.put("check", authority.getCheck());
                
                List<Map<String, Integer>> authListResponse = new ArrayList<>();
                authListResponse.add(authMapResponse);
                
                authorityMap.put("auth", authListResponse);
                authorityList.add(authorityMap);
            }
            
            // 返回成功响应
            response.put("success", true);
            response.put("message", "处理权限信息成功");
            response.put("data", authorityList);
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("处理权限信息失败：数字格式错误", e);
            response.put("success", false);
            response.put("message", "数字格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("处理权限信息失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "处理权限信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("处理权限信息失败：系统错误", e);
            response.put("success", false);
            response.put("message", "处理权限信息失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有部门和对应的身份信息
     * @return 部门和身份信息列表
     */
    @GetMapping("/departments-with-statuses")
    public ResponseEntity<Map<String, Object>> getDepartmentsWithStatuses() {
        // 使用LinkedHashMap保证响应字段顺序
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 调用封装好的方法获取部门和身份信息
            List<Map<String, Object>> departmentsWithStatuses = getDepartmentsWithStatusesList();
            
            // 返回成功响应
            response.put("success", true);
            response.put("message", "获取部门和身份信息成功");
            response.put("data", departmentsWithStatuses);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("获取部门和身份信息失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取部门和身份信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取部门和身份信息失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取部门和身份信息失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 增加、删除、修改部门或身份
     * @param request 请求体，包含token和data字段
     * @return 处理结果及更新后的部门和身份信息
     */
    @PostMapping("/manage-departments-statuses")
    public ResponseEntity<Map<String, Object>> manageDepartmentsAndStatuses(
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // 验证请求参数
            if (request == null || request.get("data") == null) {
                logger.error("处理部门和身份信息失败：请求数据不能为空");
                response.put("success", false);
                response.put("message", "请求数据不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取data字段，它应该是一个包含部门和身份信息的列表
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) request.get("data");

            // 处理每个部门
            for (Map<String, Object> departmentMap : dataList) {
                Long depId = Long.parseLong(departmentMap.get("dep_id").toString());
                String depName = (String) departmentMap.get("dep_name");
                List<Map<String, Object>> statusesList = (List<Map<String, Object>>) departmentMap.get("statuses");

                if (depId == 0) {
                    // 添加新部门前先检查是否已存在同名部门
                    Department existingDepartment = departmentRepository.findByDepName(depName);
                    if (existingDepartment == null) {
                        // 部门不存在，执行添加
                        Department newDepartment = new Department();
                        newDepartment.setDepName(depName);
                        departmentRepository.save(newDepartment);
                        logger.info("成功添加部门：{}", depName);

                        // 处理该部门下的新身份
                        if (statusesList != null) {
                            for (Map<String, Object> statusMap : statusesList) {
                                Long staId = Long.parseLong(statusMap.get("sta_id").toString());
                                String staName = (String) statusMap.get("sta_name");

                                if (staId == 0) {
                                    // 添加新身份前先检查是否已存在同名身份
                                    Status existingStatus = statusRepository.findByDepartmentIdAndStaName(newDepartment.getId(), staName);
                                    if (existingStatus == null) {
                                        // 身份不存在，执行添加
                                        Status newStatus = new Status();
                                        newStatus.setStaName(staName);
                                        newStatus.setDepartment(newDepartment);
                                        statusRepository.save(newStatus);
                                        logger.info("成功添加身份：{}（部门：{}）", staName, depName);
                                    } else {
                                        // 身份已存在，跳过添加
                                        logger.info("跳过添加身份：{}（部门：{}）- 已存在", staName, depName);
                                    }
                                }
                            }
                        }
                    } else {
                        // 部门已存在，跳过添加
                        logger.info("跳过添加部门：{}- 已存在", depName);
                    }
                } else if (depId == -1) {
                    // 删除部门（同时删除关联的身份）
                    // depId为-1时，需要通过部门名称来查找部门
                    List<Department> departments = departmentRepository.findAll().stream()
                            .filter(d -> d.getDepName().equals(depName))
                            .toList();

                    for (Department department : departments) {
                        // 检查user_info表中是否有使用该部门的记录
                        boolean existsInUserInfo = userInfoRepository.existsByDepId(department.getId());
                        if (existsInUserInfo) {
                            logger.error("删除部门失败：存在未删除的绑定信息（部门：{}）", depName);
                            response.put("success", false);
                            response.put("message", "存在未删除的绑定信息");
                            return ResponseEntity.badRequest().body(response);
                        }

                        // 检查physical_resource表中是否有使用该部门的记录
                        boolean existsInPhysicalResource = physicalResourceRepository.findAll().stream()
                                .anyMatch(pr -> pr.getDepId() != null && (
                                        pr.getDepId().equals(department.getId().toString()) ||
                                        (pr.getDepId().contains(",") && 
                                         java.util.Arrays.stream(pr.getDepId().split(","))
                                                 .map(String::trim)
                                                 .anyMatch(id -> id.equals(department.getId().toString()))
                                        )
                                ));
                        if (existsInPhysicalResource) {
                            logger.error("删除部门失败：存在未删除的绑定信息（部门：{}）", depName);
                            response.put("success", false);
                            response.put("message", "存在未删除的绑定信息");
                            return ResponseEntity.badRequest().body(response);
                        }

                        // 检查space_resource表中是否有使用该部门的记录
                        boolean existsInSpaceResource = spaceResourceRepository.findAll().stream()
                                .anyMatch(sr -> sr.getDepId() != null && (
                                        sr.getDepId().equals(department.getId().toString()) ||
                                        (sr.getDepId().contains(",") && 
                                         java.util.Arrays.stream(sr.getDepId().split(","))
                                                 .map(String::trim)
                                                 .anyMatch(id -> id.equals(department.getId().toString()))
                                        )
                                ));
                        if (existsInSpaceResource) {
                            logger.error("删除部门失败：存在未删除的绑定信息（部门：{}）", depName);
                            response.put("success", false);
                            response.put("message", "存在未删除的绑定信息");
                            return ResponseEntity.badRequest().body(response);
                        }

                        // 先删除该部门下的所有身份
                        List<Status> statuses = statusRepository.findAll().stream()
                                .filter(status -> status.getDepartment().getId().equals(department.getId()))
                                .toList();
                        statusRepository.deleteAll(statuses);

                        // 再删除部门
                        departmentRepository.delete(department);
                        logger.info("成功删除部门：{}及其所有身份", depName);
                    }
                } else {
                    // 更新部门名称
                    Department department = departmentRepository.findById(depId).orElse(null);
                    if (department != null) {
                        department.setDepName(depName);
                        departmentRepository.save(department);
                        logger.info("成功更新部门名称：{}（ID：{}）", depName, depId);

                        // 处理该部门下的身份
                        if (statusesList != null) {
                            for (Map<String, Object> statusMap : statusesList) {
                                Long staId = Long.parseLong(statusMap.get("sta_id").toString());
                                String staName = (String) statusMap.get("sta_name");

                                if (staId == 0) {
                                    // 添加新身份前先检查是否已存在同名身份
                                    Status existingStatus = statusRepository.findByDepartmentIdAndStaName(department.getId(), staName);
                                    if (existingStatus == null) {
                                        // 身份不存在，执行添加
                                        Status newStatus = new Status();
                                        newStatus.setStaName(staName);
                                        newStatus.setDepartment(department);
                                        statusRepository.save(newStatus);
                                        logger.info("成功添加身份：{}（部门：{}）", staName, depName);
                                    } else {
                                        // 身份已存在，跳过添加
                                        logger.info("跳过添加身份：{}（部门：{}）- 已存在", staName, depName);
                                    }
                                } else if (staId == -1) {
                                    // 删除身份：根据身份名称和所属部门查找并删除
                                    Status status = statusRepository.findByDepartmentIdAndStaName(department.getId(), staName);
                                    if (status != null) {
                                        // 检查user_info表中是否有使用该身份的记录
                                        boolean existsInUserInfo = userInfoRepository.existsByStaId(status.getId());
                                        if (existsInUserInfo) {
                                            // 存在未删除的绑定信息，返回错误
                                            logger.error("删除身份失败：存在未删除的绑定信息（身份：{}，部门：{}）", staName, depName);
                                            response.put("success", false);
                                            response.put("message", "存在未删除的绑定信息");
                                            return ResponseEntity.badRequest().body(response);
                                        }
                                        // 不存在绑定信息，执行删除
                                        statusRepository.delete(status);
                                        logger.info("成功删除身份：{}（部门：{}）", staName, depName);
                                    } else {
                                        logger.info("未找到要删除的身份：{}（部门：{}）", staName, depName);
                                    }
                                } else {
                                    // 更新身份名称
                                    Status status = statusRepository.findById(staId).orElse(null);
                                    if (status != null) {
                                        status.setStaName(staName);
                                        statusRepository.save(status);
                                        logger.info("成功更新身份名称：{}（ID：{}）", staName, staId);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 查询更新后的所有部门和身份信息
            List<Map<String, Object>> departmentsWithStatuses = getDepartmentsWithStatusesList();

            // 返回成功响应
            response.put("success", true);
            response.put("message", "处理部门和身份信息成功");
            response.put("data", departmentsWithStatuses);
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            logger.error("处理部门和身份信息失败：ID格式错误", e);
            response.put("success", false);
            response.put("message", "ID格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("处理部门和身份信息失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "处理部门和身份信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("处理部门和身份信息失败：系统错误", e);
            response.put("success", false);
            response.put("message", "处理部门和身份信息失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}