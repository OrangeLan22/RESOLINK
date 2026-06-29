package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.Department;
import com.orangelan.resolinkserver.entity.Authority;
import com.orangelan.resolinkserver.entity.Status;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.repository.AdminAccountRepository;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.repository.AuthorityRepository;
import com.orangelan.resolinkserver.repository.StatusRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import com.orangelan.resolinkserver.util.RandomUtil;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 账号管理控制器
 * 用于处理账号相关的API请求
 */
@RestController
@RequestMapping("/api/account-mgm")
public class AccountMgmController {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountMgmController.class);
    
    private final DepartmentRepository departmentRepository;
    private final StatusRepository statusRepository;
    private final AuthorityRepository authorityRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserInfoRepository userInfoRepository;
    private final AdminAccountRepository adminAccountRepository;
    
    public AccountMgmController(DepartmentRepository departmentRepository, StatusRepository statusRepository, 
                               AuthorityRepository authorityRepository, UserAccountRepository userAccountRepository, 
                               UserInfoRepository userInfoRepository, AdminAccountRepository adminAccountRepository) {
        this.departmentRepository = departmentRepository;
        this.statusRepository = statusRepository;
        this.authorityRepository = authorityRepository;
        this.userAccountRepository = userAccountRepository;
        this.userInfoRepository = userInfoRepository;
        this.adminAccountRepository = adminAccountRepository;
    }
    
    /**
     * 创建账号API
     * @param request 请求体，包含创建账号所需的数据
     * @return 创建结果
     */
    @PostMapping("/create-account")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("创建账号失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("创建账号失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("创建账号失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("emp_id")) {
                logger.error("创建账号失败：data字段缺少emp_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("name")) {
                logger.error("创建账号失败：data字段缺少name");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("useracc")) {
                logger.error("创建账号失败：data字段缺少useracc");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("dep_id")) {
                logger.error("创建账号失败：data字段缺少dep_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("sta_id")) {
                logger.error("创建账号失败：data字段缺少sta_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("auth_id")) {
                logger.error("创建账号失败：data字段缺少auth_id");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证必要字段的值
            String empId = data.get("emp_id") instanceof String ? (String) data.get("emp_id") : null;
            if (empId == null || empId.trim().isEmpty()) {
                logger.error("创建账号失败：emp_id不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            String name = data.get("name") instanceof String ? (String) data.get("name") : null;
            if (name == null || name.trim().isEmpty()) {
                logger.error("创建账号失败：name不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            String useracc = data.get("useracc") instanceof String ? (String) data.get("useracc") : null;
            if (useracc == null || useracc.trim().isEmpty()) {
                logger.error("创建账号失败：useracc不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long depId = null;
            Object depIdObj = data.get("dep_id");
            if (depIdObj instanceof Number) {
                depId = ((Number) depIdObj).longValue();
            } else if (depIdObj instanceof String) {
                try {
                    depId = Long.parseLong((String) depIdObj);
                } catch (NumberFormatException e) {
                    logger.error("创建账号失败：dep_id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (depId == null) {
                logger.error("创建账号失败：dep_id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long staId = null;
            Object staIdObj = data.get("sta_id");
            if (staIdObj instanceof Number) {
                staId = ((Number) staIdObj).longValue();
            } else if (staIdObj instanceof String) {
                try {
                    staId = Long.parseLong((String) staIdObj);
                } catch (NumberFormatException e) {
                    logger.error("创建账号失败：sta_id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (staId == null) {
                logger.error("创建账号失败：sta_id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long authId = null;
            Object authIdObj = data.get("auth_id");
            if (authIdObj instanceof Number) {
                authId = ((Number) authIdObj).longValue();
            } else if (authIdObj instanceof String) {
                try {
                    authId = Long.parseLong((String) authIdObj);
                } catch (NumberFormatException e) {
                    logger.error("创建账号失败：auth_id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (authId == null) {
                logger.error("创建账号失败：auth_id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证员工工号是否已存在（UserAccount表）
            if (userAccountRepository.existsByEmpId(empId)) {
                logger.error("创建账号失败：员工工号 {} 已存在", empId);
                response.put("success", false);
                response.put("message", "员工工号已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证员工工号是否已存在（UserInfo表）
            if (userInfoRepository.existsByEmpId(empId)) {
                logger.error("创建账号失败：员工工号 {} 已存在", empId);
                response.put("success", false);
                response.put("message", "员工工号已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证账号是否已存在
            if (userAccountRepository.existsByUseracc(useracc)) {
                logger.error("创建账号失败：账号 {} 已存在", useracc);
                response.put("success", false);
                response.put("message", "账号已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证部门是否存在
            Optional<Department> departmentOptional = departmentRepository.findById(depId);
            if (!departmentOptional.isPresent()) {
                logger.error("创建账号失败：部门不存在（ID：{}", depId);
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证该部门下是否存在对应的身份
            Optional<Status> statusOptional = statusRepository.findById(staId);
            if (!statusOptional.isPresent() || !statusOptional.get().getDepartment().getId().equals(depId)) {
                logger.error("创建账号失败：身份不存在或不属于该部门（部门ID：{}，身份ID：{}", depId, staId);
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证权限是否存在且auth_id不为99
            if (authId == 99) {
                logger.error("创建账号失败：auth_id不允许为99");
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<Authority> authorityOptional = authorityRepository.findById(authId);
            if (!authorityOptional.isPresent()) {
                logger.error("创建账号失败：权限不存在（ID：{}", authId);
                response.put("success", false);
                response.put("message", "非法的数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 生成盐（8位随机英文字母）
            String salt = RandomUtil.generateSalt();
            
            // 生成默认密码（8位随机字母数字混合）
            String defaultPassword = RandomUtil.generateDefaultPassword();
            
            // 对密码进行哈希处理
            String hashedPassword = RandomUtil.hashPassword(defaultPassword, salt);
            
            // 创建UserAccount实体
            UserAccount userAccount = new UserAccount();
            userAccount.setEmpId(empId);
            userAccount.setUseracc(useracc);
            userAccount.setUserpass(hashedPassword);
            userAccount.setSalt(salt);
            userAccount.setIsinitial(1);
            
            // 创建UserInfo实体
            UserInfo userInfo = new UserInfo();
            userInfo.setEmpId(empId);
            userInfo.setName(name);
            userInfo.setDepId(depId);
            userInfo.setStaId(staId);
            userInfo.setAuthId(authId);
            
            // 保存到数据库
            userAccountRepository.save(userAccount);
            userInfoRepository.save(userInfo);
            
            logger.info("账号创建成功：员工工号：{}，账号：{}", empId, useracc);
            
            // 构造响应
            response.put("success", true);
            response.put("message", "账号创建成功");
            Map<String, String> accountInfo = new LinkedHashMap<>();
            accountInfo.put("useracc", useracc);
            accountInfo.put("password", defaultPassword);
            response.put("data", accountInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("创建账号失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("创建账号失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "账号创建失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("创建账号失败：系统错误", e);
            response.put("success", false);
            response.put("message", "账号创建失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取账号与身份信息的API
     * @param size 每页显示的个数
     * @param page 页数
     * @return 账号与身份信息列表
     */
    @GetMapping("/get-accounts")
    public ResponseEntity<Map<String, Object>> getAccounts(@RequestHeader("size") Integer size, @RequestHeader("page") Integer page) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证参数
            if (size == null || size <= 0) {
                logger.error("获取账号信息失败：size参数无效");
                response.put("success", false);
                response.put("message", "size参数无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (page == null || page < 1) {
                logger.error("获取账号信息失败：page参数无效");
                response.put("success", false);
                response.put("message", "page参数无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 将page参数从1开始转换为0开始
            int jpaPage = page - 1;
            
            // 创建分页请求
            Pageable pageable = PageRequest.of(jpaPage, size);
            
            // 查询useraccount表
            Page<UserAccount> userAccountPage = userAccountRepository.findAll(pageable);
            List<UserAccount> userAccounts = userAccountPage.getContent();
            
            // 准备结果列表
            List<Map<String, Object>> resultList = new ArrayList<>();
            
            // 遍历查询结果，关联查询user_info表及相关信息
            for (UserAccount userAccount : userAccounts) {
                // 根据emp_id查询user_info表
                UserInfo userInfo = userInfoRepository.findByEmpId(userAccount.getEmpId());
                if (userInfo != null) {
                    // 根据dep_id查询部门名称
                    Optional<Department> departmentOptional = departmentRepository.findById(userInfo.getDepId());
                    String depName = departmentOptional.map(Department::getDepName).orElse("");
                    
                    // 根据sta_id查询身份名称
                    Optional<Status> statusOptional = statusRepository.findById(userInfo.getStaId());
                    String staName = statusOptional.map(Status::getStaName).orElse("");
                    
                    // 根据auth_id查询权限标签
                    Optional<Authority> authorityOptional = authorityRepository.findById(userInfo.getAuthId());
                    String authTag = authorityOptional.map(Authority::getTag).orElse("");
                    
                    // 构建返回数据
                    Map<String, Object> accountData = new LinkedHashMap<>();
                    accountData.put("id", userAccount.getId());
                    accountData.put("emp_id", userAccount.getEmpId());
                    accountData.put("name", userInfo.getName());
                    accountData.put("useracc", userAccount.getUseracc());
                    accountData.put("dep_id", userInfo.getDepId());
                    accountData.put("dep_name", depName);
                    accountData.put("sta_id", userInfo.getStaId());
                    accountData.put("sta_name", staName);
                    accountData.put("auth_id", userInfo.getAuthId());
                    accountData.put("auth_tag", authTag);
                    
                    resultList.add(accountData);
                }
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "获取账号信息成功");
            response.put("data", resultList);
            response.put("total", userAccountPage.getTotalElements());
            response.put("totalPages", userAccountPage.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("获取账号信息失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取账号信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取账号信息失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取账号信息失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除账号的API
     * @param request 请求体，包含要删除的账号id
     * @return 删除结果
     */
    @PostMapping("/delete-account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("删除账号失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("删除账号失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("删除账号失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证id字段是否存在
            if (!data.containsKey("id")) {
                logger.error("删除账号失败：data字段缺少id");
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
                try {
                    id = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.error("删除账号失败：id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (id == null) {
                logger.error("删除账号失败：id不是有效的数字");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询useraccount记录
            Optional<UserAccount> userAccountOptional = userAccountRepository.findById(id);
            if (!userAccountOptional.isPresent()) {
                logger.error("删除账号失败：账号不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "账号不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            UserAccount userAccount = userAccountOptional.get();
            String empId = userAccount.getEmpId();
            
            // 删除user_info表中对应emp_id的记录
            UserInfo userInfo = userInfoRepository.findByEmpId(empId);
            if (userInfo != null) {
                userInfoRepository.delete(userInfo);
            }
            
            // 删除useraccount表中的记录
            userAccountRepository.delete(userAccount);
            
            logger.info("账号删除成功：ID：{}，员工工号：{}", id, empId);
            
            // 构造响应
            response.put("success", true);
            response.put("message", "账号删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("删除账号失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "账号删除失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("删除账号失败：系统错误", e);
            response.put("success", false);
            response.put("message", "账号删除失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据姓名或工号查询账号的API
     * @param request 请求体，包含search字段和分页参数
     * @return 查询结果
     */
    @PostMapping("/search-accounts")
    public ResponseEntity<Map<String, Object>> searchAccounts(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("查询账号失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("查询账号失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("查询账号失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("search")) {
                logger.error("查询账号失败：data字段缺少search");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("page")) {
                logger.error("查询账号失败：data字段缺少page");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!data.containsKey("size")) {
                logger.error("查询账号失败：data字段缺少size");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证search字段
            String search = data.get("search") instanceof String ? (String) data.get("search") : null;
            if (search == null || search.trim().isEmpty()) {
                logger.error("查询账号失败：search字段不能为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证page字段
            Integer page = null;
            Object pageObj = data.get("page");
            if (pageObj instanceof Number) {
                page = ((Number) pageObj).intValue();
            } else if (pageObj instanceof String) {
                try {
                    page = Integer.parseInt((String) pageObj);
                } catch (NumberFormatException e) {
                    logger.error("查询账号失败：page不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (page == null || page < 1) {
                logger.error("查询账号失败：page参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取并验证size字段
            Integer size = null;
            Object sizeObj = data.get("size");
            if (sizeObj instanceof Number) {
                size = ((Number) sizeObj).intValue();
            } else if (sizeObj instanceof String) {
                try {
                    size = Integer.parseInt((String) sizeObj);
                } catch (NumberFormatException e) {
                    logger.error("查询账号失败：size不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (size == null || size <= 0) {
                logger.error("查询账号失败：size参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 先获取所有用户账号
            List<UserAccount> allUserAccounts = userAccountRepository.findAll();
            
            // 准备所有符合条件的结果列表
            List<Map<String, Object>> allMatchingResults = new ArrayList<>();
            
            // 遍历所有用户账号，关联查询user_info表及相关信息，筛选符合条件的记录
            for (UserAccount userAccount : allUserAccounts) {
                // 根据emp_id查询user_info表
                UserInfo userInfo = userInfoRepository.findByEmpId(userAccount.getEmpId());
                if (userInfo != null) {
                    // 检查是否匹配search条件（姓名或工号）
                    if (userInfo.getName().contains(search) || userAccount.getEmpId().contains(search)) {
                        // 根据dep_id查询部门名称
                        Optional<Department> departmentOptional = departmentRepository.findById(userInfo.getDepId());
                        String depName = departmentOptional.map(Department::getDepName).orElse("");
                        
                        // 根据sta_id查询身份名称
                        Optional<Status> statusOptional = statusRepository.findById(userInfo.getStaId());
                        String staName = statusOptional.map(Status::getStaName).orElse("");
                        
                        // 根据auth_id查询权限标签
                        Optional<Authority> authorityOptional = authorityRepository.findById(userInfo.getAuthId());
                        String authTag = authorityOptional.map(Authority::getTag).orElse("");
                        
                        // 构建返回数据
                        Map<String, Object> accountData = new LinkedHashMap<>();
                        accountData.put("id", userAccount.getId());
                        accountData.put("emp_id", userAccount.getEmpId());
                        accountData.put("name", userInfo.getName());
                        accountData.put("useracc", userAccount.getUseracc());
                        accountData.put("dep_id", userInfo.getDepId());
                        accountData.put("dep_name", depName);
                        accountData.put("sta_id", userInfo.getStaId());
                        accountData.put("sta_name", staName);
                        accountData.put("auth_id", userInfo.getAuthId());
                        accountData.put("auth_tag", authTag);
                        
                        allMatchingResults.add(accountData);
                    }
                }
            }
            
            // 计算总记录数和总页数
            int total = allMatchingResults.size();
            int totalPages = (int) Math.ceil((double) total / size);
            
            // 计算分页起始和结束索引
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);
            
            // 对结果进行分页
            List<Map<String, Object>> paginatedResults = new ArrayList<>();
            if (startIndex < endIndex && startIndex < total) {
                paginatedResults = allMatchingResults.subList(startIndex, endIndex);
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "查询账号信息成功");
            response.put("data", paginatedResults);
            response.put("total", total);
            response.put("totalPages", totalPages);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("查询账号失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "查询账号失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("查询账号失败：系统错误", e);
            response.put("success", false);
            response.put("message", "查询账号失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 重置密码的API
     * @param request 请求体，包含data字段，内包括id
     * @return 重置后的账号与密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 验证请求体是否存在
            if (request == null) {
                logger.error("重置密码失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("重置密码失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof Map)) {
                logger.error("重置密码失败：data字段不是有效的JSON对象");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 验证必要字段是否存在
            if (!data.containsKey("id")) {
                logger.error("重置密码失败：data字段缺少id");
                response.put("success", false);
                response.put("message", "数据不完整");
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
                    logger.error("重置密码失败：id不是有效的数字格式");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (id == null || id <= 0) {
                logger.error("重置密码失败：id参数无效");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 根据id查询用户账号
            Optional<UserAccount> userAccountOptional = userAccountRepository.findById(id);
            if (!userAccountOptional.isPresent()) {
                logger.error("重置密码失败：账号不存在（ID：{}", id);
                response.put("success", false);
                response.put("message", "账号不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取用户账号
            UserAccount userAccount = userAccountOptional.get();
            
            // 生成新的随机盐
            String salt = RandomUtil.generateSalt();
            
            // 生成新的随机密码
            String newPassword = RandomUtil.generateDefaultPassword();
            
            // 对新密码进行哈希处理（密码 + 盐）
            String hashedPassword = RandomUtil.hashPassword(newPassword, salt);
            
            // 更新用户账号的密码和盐
            userAccount.setUserpass(hashedPassword);
            userAccount.setSalt(salt);
            userAccount.setIsinitial(1); // 重置为初始密码状态
            
            // 保存到数据库
            userAccountRepository.save(userAccount);
            
            logger.info("重置密码成功：账号ID：{}，账号：{}", id, userAccount.getUseracc());
            
            // 构造响应
            response.put("success", true);
            response.put("message", "密码重置成功");
            Map<String, String> accountInfo = new LinkedHashMap<>();
            accountInfo.put("useracc", userAccount.getUseracc());
            accountInfo.put("password", newPassword);
            response.put("data", accountInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("重置密码失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "密码重置失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("重置密码失败：系统错误", e);
            response.put("success", false);
            response.put("message", "密码重置失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 修改账号API
     * @param request 请求体，包含修改账号所需的数据
     * @return 修改结果
     */
    @PostMapping("/update-account")
    public ResponseEntity<Map<String, Object>> updateAccount(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            // 创建列表存储修改后的账号信息
            List<Map<String, Object>> resultList = new ArrayList<>();
            
            // 验证请求体是否存在
            if (request == null) {
                logger.error("修改账号失败：请求体为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证请求体是否包含data字段
            if (!request.containsKey("data")) {
                logger.error("修改账号失败：请求体缺少data字段");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段并验证类型
            Object dataObj = request.get("data");
            if (!(dataObj instanceof List)) {
                logger.error("修改账号失败：data字段不是有效的JSON数组");
                response.put("success", false);
                response.put("message", "数据格式错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取data字段
            List<?> dataList = (List<?>) dataObj;
            
            // 验证data数组是否为空
            if (dataList.isEmpty()) {
                logger.error("修改账号失败：data数组为空");
                response.put("success", false);
                response.put("message", "数据不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 遍历修改每个账号
            for (Object itemObj : dataList) {
                if (!(itemObj instanceof Map)) {
                    logger.error("修改账号失败：data数组中的元素不是有效的JSON对象");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                Map<String, Object> item = (Map<String, Object>) itemObj;
                
                // 验证必要字段是否存在
                if (!item.containsKey("id")) {
                    logger.error("修改账号失败：数据缺少id字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("emp_id")) {
                    logger.error("修改账号失败：数据缺少emp_id字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("name")) {
                    logger.error("修改账号失败：数据缺少name字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("useracc")) {
                    logger.error("修改账号失败：数据缺少useracc字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("dep_id")) {
                    logger.error("修改账号失败：数据缺少dep_id字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("sta_id")) {
                    logger.error("修改账号失败：数据缺少sta_id字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (!item.containsKey("auth_id")) {
                    logger.error("修改账号失败：数据缺少auth_id字段");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证id字段
                Long id = null;
                Object idObj = item.get("id");
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                } else if (idObj instanceof String) {
                    try {
                        id = Long.parseLong((String) idObj);
                    } catch (NumberFormatException e) {
                        logger.error("修改账号失败：id不是有效的数字格式");
                        response.put("success", false);
                        response.put("message", "数据格式错误");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                
                if (id == null) {
                    logger.error("修改账号失败：id不是有效的数字");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证emp_id字段
                String empId = item.get("emp_id") instanceof String ? (String) item.get("emp_id") : null;
                if (empId == null || empId.trim().isEmpty()) {
                    logger.error("修改账号失败：emp_id不能为空");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证name字段
                String name = item.get("name") instanceof String ? (String) item.get("name") : null;
                if (name == null || name.trim().isEmpty()) {
                    logger.error("修改账号失败：name不能为空");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证useracc字段
                String useracc = item.get("useracc") instanceof String ? (String) item.get("useracc") : null;
                if (useracc == null || useracc.trim().isEmpty()) {
                    logger.error("修改账号失败：useracc不能为空");
                    response.put("success", false);
                    response.put("message", "数据不完整");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证dep_id字段
                Long depId = null;
                Object depIdObj = item.get("dep_id");
                if (depIdObj instanceof Number) {
                    depId = ((Number) depIdObj).longValue();
                } else if (depIdObj instanceof String) {
                    try {
                        depId = Long.parseLong((String) depIdObj);
                    } catch (NumberFormatException e) {
                        logger.error("修改账号失败：dep_id不是有效的数字格式");
                        response.put("success", false);
                        response.put("message", "数据格式错误");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                
                if (depId == null) {
                    logger.error("修改账号失败：dep_id不是有效的数字");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证sta_id字段
                Long staId = null;
                Object staIdObj = item.get("sta_id");
                if (staIdObj instanceof Number) {
                    staId = ((Number) staIdObj).longValue();
                } else if (staIdObj instanceof String) {
                    try {
                        staId = Long.parseLong((String) staIdObj);
                    } catch (NumberFormatException e) {
                        logger.error("修改账号失败：sta_id不是有效的数字格式");
                        response.put("success", false);
                        response.put("message", "数据格式错误");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                
                if (staId == null) {
                    logger.error("修改账号失败：sta_id不是有效的数字");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取并验证auth_id字段
                Long authId = null;
                Object authIdObj = item.get("auth_id");
                if (authIdObj instanceof Number) {
                    authId = ((Number) authIdObj).longValue();
                } else if (authIdObj instanceof String) {
                    try {
                        authId = Long.parseLong((String) authIdObj);
                    } catch (NumberFormatException e) {
                        logger.error("修改账号失败：auth_id不是有效的数字格式");
                        response.put("success", false);
                        response.put("message", "数据格式错误");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                
                if (authId == null) {
                    logger.error("修改账号失败：auth_id不是有效的数字");
                    response.put("success", false);
                    response.put("message", "数据格式错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 根据id查询useraccount记录
                Optional<UserAccount> userAccountOptional = userAccountRepository.findById(id);
                if (!userAccountOptional.isPresent()) {
                    logger.error("修改账号失败：账号不存在（ID：{}", id);
                    response.put("success", false);
                    response.put("message", "账号不存在");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 获取原userAccount记录
                UserAccount userAccount = userAccountOptional.get();
                String originalEmpId = userAccount.getEmpId();
                
                // 根据原emp_id查询user_info表
                UserInfo userInfo = userInfoRepository.findByEmpId(originalEmpId);
                if (userInfo == null) {
                    logger.error("修改账号失败：用户信息不存在（原员工工号：{}", originalEmpId);
                    response.put("success", false);
                    response.put("message", "用户信息不存在");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 验证部门是否存在
                Optional<Department> departmentOptional = departmentRepository.findById(depId);
                if (!departmentOptional.isPresent()) {
                    logger.error("修改账号失败：部门不存在（ID：{}", depId);
                    response.put("success", false);
                    response.put("message", "非法的数据");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 验证该部门下是否存在对应的身份
                Optional<Status> statusOptional = statusRepository.findById(staId);
                if (!statusOptional.isPresent() || !statusOptional.get().getDepartment().getId().equals(depId)) {
                    logger.error("修改账号失败：身份不存在或不属于该部门（部门ID：{}，身份ID：{}", depId, staId);
                    response.put("success", false);
                    response.put("message", "非法的数据");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 验证权限是否存在且auth_id不为99
                if (authId == 99) {
                    logger.error("修改账号失败：auth_id不允许为99");
                    response.put("success", false);
                    response.put("message", "非法的数据");
                    return ResponseEntity.badRequest().body(response);
                }
                
                Optional<Authority> authorityOptional = authorityRepository.findById(authId);
                if (!authorityOptional.isPresent()) {
                    logger.error("修改账号失败：权限不存在（ID：{}", authId);
                    response.put("success", false);
                    response.put("message", "非法的数据");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 检查emp_id是否被修改，如果修改了，需要验证新的emp_id是否已存在
                if (!empId.equals(originalEmpId)) {
                    // 验证员工工号是否已存在（UserAccount表）
                    if (userAccountRepository.existsByEmpId(empId)) {
                        logger.error("修改账号失败：员工工号 {} 已存在", empId);
                        response.put("success", false);
                        response.put("message", "员工工号已存在");
                        return ResponseEntity.badRequest().body(response);
                    }
                    
                    // 验证员工工号是否已存在（UserInfo表）
                    if (userInfoRepository.existsByEmpId(empId)) {
                        logger.error("修改账号失败：员工工号 {} 已存在", empId);
                        response.put("success", false);
                        response.put("message", "员工工号已存在");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                
                // 检查账号是否已存在（排除当前账号）
                if (!useracc.equals(userAccount.getUseracc()) && userAccountRepository.existsByUseracc(useracc)) {
                    logger.error("修改账号失败：账号 {} 已存在", useracc);
                    response.put("success", false);
                    response.put("message", "账号已存在");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 更新userAccount记录
                userAccount.setEmpId(empId);
                userAccount.setUseracc(useracc);
                
                // 更新userInfo记录
                userInfo.setEmpId(empId);
                userInfo.setName(name);
                userInfo.setDepId(depId);
                userInfo.setStaId(staId);
                userInfo.setAuthId(authId);
                
                // 保存到数据库
                userAccountRepository.save(userAccount);
                userInfoRepository.save(userInfo);
                
                // 查询部门名称
                Optional<Department> deptOpt = departmentRepository.findById(depId);
                String depName = deptOpt.map(Department::getDepName).orElse("");
                
                // 查询身份名称
                Optional<Status> statusOpt = statusRepository.findById(staId);
                String staName = statusOpt.map(Status::getStaName).orElse("");
                
                // 查询权限标签
                Optional<Authority> authOpt = authorityRepository.findById(authId);
                String authTag = authOpt.map(Authority::getTag).orElse("");
                
                // 构建返回数据
                Map<String, Object> accountData = new LinkedHashMap<>();
                accountData.put("id", id);
                accountData.put("emp_id", empId);
                accountData.put("name", name);
                accountData.put("useracc", useracc);
                accountData.put("dep_id", depId);
                accountData.put("dep_name", depName);
                accountData.put("sta_id", staId);
                accountData.put("sta_name", staName);
                accountData.put("auth_id", authId);
                accountData.put("auth_tag", authTag);
                
                // 添加到结果列表
                resultList.add(accountData);
                
                logger.info("账号修改成功：账号ID：{}，员工工号：{}", id, empId);
            }
            
            // 构造响应
            response.put("success", true);
            response.put("message", "账号修改成功");
            response.put("data", resultList);
            
            return ResponseEntity.ok(response);
            
        } catch (ClassCastException e) {
            logger.error("修改账号失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("修改账号失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "账号修改失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("修改账号失败：系统错误", e);
            response.put("success", false);
            response.put("message", "修改账号失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据useraccount获取用户ID
     * @param request 包含useraccount的请求参数
     * @return 用户ID
     */
    @PostMapping("/get-user-id-by-account")
    public ResponseEntity<Map<String, Object>> getUserIdByAccount(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        try {
            if (request == null || !request.containsKey("useraccount")) {
                response.put("success", false);
                response.put("message", "缺少必要的参数");
                return ResponseEntity.badRequest().body(response);
            }
            
            String userAccount = (String) request.get("useraccount");
            
            // 首先检查是否为管理员账号
            AdminAccount adminAccount = adminAccountRepository.findByAdacc(userAccount);
            if (adminAccount != null) {
                // 如果是管理员，根据emp_id在user_info表中查找
                UserInfo userInfo = userInfoRepository.findByEmpId(adminAccount.getEmpId());
                if (userInfo != null) {
                    response.put("success", true);
                    response.put("userId", userInfo.getId());
                    return ResponseEntity.ok(response);
                }
            } else {
                // 如果不是管理员，根据useracc在useraccount表中查找，然后根据emp_id在user_info表中查找
                UserAccount userAcc = userAccountRepository.findByUseracc(userAccount);
                if (userAcc != null) {
                    UserInfo userInfo = userInfoRepository.findByEmpId(userAcc.getEmpId());
                    if (userInfo != null) {
                        response.put("success", true);
                        response.put("userId", userInfo.getId());
                        return ResponseEntity.ok(response);
                    }
                }
            }
            
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(response);
            
        } catch (ClassCastException e) {
            logger.error("获取用户ID失败：数据类型错误", e);
            response.put("success", false);
            response.put("message", "非法的数据");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            logger.error("获取用户ID失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "获取用户ID失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("获取用户ID失败：系统错误", e);
            response.put("success", false);
            response.put("message", "获取用户ID失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}