package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.dto.LoginRequest;
import com.orangelan.resolinkserver.dto.LoginResponse;
import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.Department;
import com.orangelan.resolinkserver.entity.Status;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.repository.DepartmentRepository;
import com.orangelan.resolinkserver.repository.StatusRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import com.orangelan.resolinkserver.service.LoginService;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final LoginService loginService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserInfoRepository userInfoRepository;
    private final DepartmentRepository departmentRepository;
    private final StatusRepository statusRepository;

    public LoginController(LoginService loginService, JwtTokenUtil jwtTokenUtil, UserInfoRepository userInfoRepository, DepartmentRepository departmentRepository, StatusRepository statusRepository) {
        this.loginService = loginService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userInfoRepository = userInfoRepository;
        this.departmentRepository = departmentRepository;
        this.statusRepository = statusRepository;
    }

    /**
     * 登录接口
     * @param loginRequest 登录请求参数
     * @return 登录响应
     */
    @PostMapping
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        try {
            // 验证请求对象
            if (loginRequest == null) {
                logger.error("登录请求参数不能为空");
                return new LoginResponse(false, "登录请求参数不能为空");
            }
            
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            String clientId = loginRequest.getClientId();
            
            // 验证必填参数
            if (username == null || username.trim().isEmpty()) {
                logger.error("用户名不能为空");
                return new LoginResponse(false, "用户名不能为空");
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.error("密码不能为空");
                return new LoginResponse(false, "密码不能为空");
            }
            
            if (clientId == null || clientId.trim().isEmpty()) {
                logger.error("客户端标识不能为空");
                return new LoginResponse(false, "客户端标识不能为空");
            }
            
            String finalUsername = username.trim();
            String finalPassword = password.trim();
            String finalClientId = clientId.trim();
            
            logger.info("用户 {} 尝试登录，客户端ID：{}", finalUsername, finalClientId);
            
            boolean isLoginSuccess = loginService.login(finalUsername, finalPassword, finalClientId);
            
            if (isLoginSuccess) {
                logger.info("用户 {} 登录成功，客户端ID：{}", finalUsername, finalClientId);
                
                // 获取管理员账户信息
                AdminAccount adminAccount = loginService.getAdminAccountByUsername(finalUsername);
                
                // 判断是否为Admin用户，如果是则authority为99，否则为0
                int authority = 0;
                UserInfo userInfo = null;
                
                if (adminAccount != null) {
                    authority = 99; // Admin用户
                    // 查询userinfo表中与adminaccount的employeeid相符和的行数据
                    userInfo = userInfoRepository.findByEmpId(adminAccount.getEmpId());
                } else {
                    // 如果是普通用户，从useraccount表中获取emp_id
                    UserAccount userAccount = loginService.getUserAccountByUsername(finalUsername);
                    if (userAccount != null) {
                        // 根据emp_id在user_info表中查找对应的权限信息
                        userInfo = userInfoRepository.findByEmpId(userAccount.getEmpId());
                        if (userInfo != null) {
                            // 根据auth_id设置权限级别
                            authority = userInfo.getAuthId().intValue();
                        }
                    }
                }
                
                // 生成Token，包含权限信息
                String token = jwtTokenUtil.generateToken(finalUsername, authority);
                
                // 查询部门信息
                if (userInfo != null && userInfo.getDepId() != null) {
                    Department department = departmentRepository.findById(userInfo.getDepId()).orElse(null);
                    if (department != null) {
                        userInfo.setDepName(department.getDepName());
                    }
                }
                
                // 查询身份信息
                if (userInfo != null && userInfo.getStaId() != null) {
                    Status status = statusRepository.findById(userInfo.getStaId()).orElse(null);
                    if (status != null) {
                        userInfo.setStaName(status.getStaName());
                    }
                }
                
                // 返回包含Token、用户信息和权限的响应
                return new LoginResponse(true, "登录成功", token, userInfo, authority);
            } else {
                logger.info("用户 {} 登录失败：用户名或密码错误，客户端ID：{}", finalUsername, finalClientId);
                return new LoginResponse(false, "用户名或密码错误");
            }
        } catch (IllegalArgumentException e) {
            logger.error("登录失败：参数验证错误", e);
            return new LoginResponse(false, "非法的加密格式");
        } catch (RuntimeException e) {
            logger.error("登录失败：业务逻辑错误", e);
            return new LoginResponse(false, "登录失败");
        } catch (Exception e) {
            logger.error("登录失败：系统错误", e);
            return new LoginResponse(false, "登录失败：系统内部错误，请稍后重试");
        }
    }
}