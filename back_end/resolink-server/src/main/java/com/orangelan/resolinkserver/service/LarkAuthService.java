package com.orangelan.resolinkserver.service;

import com.lark.oapi.Client;
import com.lark.oapi.core.Config;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.authen.v1.model.CreateAccessTokenReq;
import com.lark.oapi.service.authen.v1.model.CreateAccessTokenReqBody;
import com.lark.oapi.service.authen.v1.model.CreateAccessTokenResp;
import com.lark.oapi.service.contact.v3.model.GetUserReq;
import com.lark.oapi.service.contact.v3.model.GetUserResp;
import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.repository.AdminAccountRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LarkAuthService {
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private AdminAccountRepository adminAccountRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private LarkMessageService larkMessageService;
    
    @Value("${lark.appId}")
    private String appId;
    
    @Value("${lark.appSecret}")
    private String appSecret;
    
    @Value("${lark.redirectUri}")
    private String redirectUri;
    
    @Value("${feishu.card_id.bind}")
    private String bindCardId;
    
    @Value("${feishu.card_id.unbind}")
    private String unbindCardId;
    
    /**
     * 生成飞书授权URL
     * @param state 状态参数，用于防止CSRF攻击
     * @return 飞书授权URL
     */
    public String generateAuthUrl(String state) {
        return String.format(
            "https://open.feishu.cn/open-apis/authen/v1/index?redirect_uri=%s&app_id=%s&state=%s",
            redirectUri,
            appId,
            state
        );
    }
    
    /**
     * 处理飞书授权回调，获取access_token并绑定用户
     * @param code 飞书授权码
     * @param userId 系统用户ID
     * @return 是否绑定成功
     */
    @Transactional
    public boolean handleAuthCallback(String code, Long userId) {
        try {
            System.out.println("开始处理飞书授权回调，userId: " + userId);
            
            // 初始化飞书客户端
            Client client = Client.newBuilder(appId, appSecret).build();
            System.out.println("飞书客户端初始化成功");
            
            // 获取access_token
            CreateAccessTokenReq req = new CreateAccessTokenReq();
            CreateAccessTokenReqBody reqBody = new CreateAccessTokenReqBody();
            reqBody.setCode(code);
            reqBody.setGrantType("authorization_code");
            req.setCreateAccessTokenReqBody(reqBody);
            
            System.out.println("开始获取access_token");
            CreateAccessTokenResp resp = client.authen().v1().accessToken().create(req);
            System.out.println("获取access_token响应: code=" + resp.getCode() + ", msg=" + resp.getMsg());
            
            if (resp.getCode() != 0) {
                System.err.println("获取access_token失败: " + resp.getMsg());
                return false;
            }
            
            String accessToken = resp.getData().getAccessToken();
            String openId = resp.getData().getOpenId();
            String userIdLark = resp.getData().getUserId();
            
            System.out.println("获取access_token成功，userIdLark: " + userIdLark + ", openId: " + openId);
            
            // 使用openId作为飞书用户ID，因为userIdLark可能为null
            String feishuUserId = openId;
            
            // 直接绑定用户，跳过获取用户信息的步骤
            // 因为获取用户信息需要额外的权限
            
            // 绑定用户
            System.out.println("开始绑定用户，userId: " + userId);
            UserInfo userInfo = userInfoRepository.findById(userId).orElse(null);
            if (userInfo == null) {
                System.err.println("用户不存在: " + userId);
                return false;
            }
            
            System.out.println("找到用户: " + userInfo.getName() + ", 当前feishuUserId: " + userInfo.getFeishuUserId());
            // 只更新feishuUserId字段，避免其他字段被意外修改
            userInfoRepository.updateFeishuUserIdById(feishuUserId, userId);
            System.out.println("用户绑定成功，feishuUserId: " + feishuUserId);
            
            // 向用户发送绑定成功的消息（卡片消息）
            String userName = userInfo.getName();
            String userAccount = "";
            
            // 获取用户账号
            UserAccount userAcc = userAccountRepository.findByEmpId(userInfo.getEmpId());
            if (userAcc != null) {
                userAccount = userAcc.getUseracc();
            } else {
                // 尝试从管理员账号中查找
                java.util.List<AdminAccount> adminAccounts = adminAccountRepository.findAll();
                for (AdminAccount adminAccount : adminAccounts) {
                    if (adminAccount.getEmpId().equals(userInfo.getEmpId())) {
                        userAccount = adminAccount.getAdacc();
                        break;
                    }
                }
            }
            
            // 构建卡片消息内容
            String cardContent = String.format("{\"type\":\"template\",\"data\":{\"template_id\":\"%s\",\"template_variable\":{\"USER_NAME\":\"%s\",\"USER_ACCOUNT\":\"%s\"}}}", bindCardId, userName, userAccount);
            
            boolean sendResult = larkMessageService.sendCardMessage(feishuUserId, cardContent, "open_id");
            System.out.println("发送绑定成功卡片消息结果: " + sendResult);
            
            return true;
        } catch (Exception e) {
            System.err.println("处理飞书授权回调异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 生成随机state参数
     * @return 随机state字符串
     */
    public String generateState() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 根据useraccount查找用户信息
     * @param userAccount 用户名
     * @return UserInfo对象
     */
    public UserInfo findUserInfoByUserAccount(String userAccount) {
        try {
            // 首先检查是否为管理员账号
            AdminAccount adminAccount = adminAccountRepository.findByAdacc(userAccount);
            if (adminAccount != null) {
                // 如果是管理员，根据emp_id在user_info表中查找
                return userInfoRepository.findByEmpId(adminAccount.getEmpId());
            } else {
                // 如果不是管理员，根据useracc在useraccount表中查找，然后根据emp_id在user_info表中查找
                UserAccount userAcc = userAccountRepository.findByUseracc(userAccount);
                if (userAcc != null) {
                    return userInfoRepository.findByEmpId(userAcc.getEmpId());
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("根据useraccount查找用户信息异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据用户ID获取飞书用户ID
     * @param userId 系统用户ID
     * @return 飞书用户ID
     */
    public String getFeishuUserIdByUserId(Long userId) {
        UserInfo userInfo = userInfoRepository.findById(userId).orElse(null);
        return userInfo != null ? userInfo.getFeishuUserId() : null;
    }
    
    /**
     * 解除用户与飞书账号的绑定
     * @param userId 系统用户ID
     * @return 是否解除绑定成功
     */
    @Transactional
    public boolean unbindFeishuAccount(Long userId) {
        try {
            UserInfo userInfo = userInfoRepository.findById(userId).orElse(null);
            if (userInfo == null) {
                System.err.println("用户不存在: " + userId);
                return false;
            }
            
            // 获取飞书用户ID，用于发送解绑消息
            String feishuUserId = userInfo.getFeishuUserId();
            
            // 只更新feishuUserId字段为null，避免修改其他字段
            userInfoRepository.updateFeishuUserIdById(null, userId);
            
            // 向用户发送解绑成功的消息（卡片消息）
            if (feishuUserId != null) {
                String userName = userInfo.getName();
                String userAccount = "";
                
                // 获取用户账号
                UserAccount userAcc = userAccountRepository.findByEmpId(userInfo.getEmpId());
                if (userAcc != null) {
                    userAccount = userAcc.getUseracc();
                } else {
                    // 尝试从管理员账号中查找
                    java.util.List<AdminAccount> adminAccounts = adminAccountRepository.findAll();
                    for (AdminAccount adminAccount : adminAccounts) {
                        if (adminAccount.getEmpId().equals(userInfo.getEmpId())) {
                            userAccount = adminAccount.getAdacc();
                            break;
                        }
                    }
                }
                
                // 构建卡片消息内容
                String cardContent = String.format("{\"type\":\"template\",\"data\":{\"template_id\":\"%s\",\"template_variable\":{\"USER_NAME\":\"%s\",\"USER_ACCOUNT\":\"%s\"}}}", unbindCardId, userName, userAccount);
                
                boolean sendResult = larkMessageService.sendCardMessage(feishuUserId, cardContent, "open_id");
                System.out.println("发送解绑成功卡片消息结果: " + sendResult);
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("解除绑定异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取飞书应用的access_token
     * @return access_token
     */
    private String getAppAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            String url = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal/";
            
            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("app_id", appId);
            reqBody.put("app_secret", appSecret);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(reqBody, headers);
            
            Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class).getBody();
            
            if (resp != null && resp.get("code").equals(0)) {
                return (String) resp.get("app_access_token");
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("获取应用access_token异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 通过手机号或邮箱获取用户的open_id
     * @param emails 邮箱列表
     * @param mobiles 手机号列表
     * @return 用户的open_id
     */
    public String getOpenIdByEmailOrMobile(String[] emails, String[] mobiles) {
        try {
            // 获取应用access_token
            String accessToken = getAppAccessToken();
            if (accessToken == null) {
                System.err.println("获取应用access_token失败");
                return null;
            }
            
            RestTemplate restTemplate = new RestTemplate();
            
            String url = "https://open.feishu.cn/open-apis/contact/v3/users/batch_get_id?user_id_type=open_id";
            
            Map<String, Object> reqBody = new HashMap<>();
            if (emails != null && emails.length > 0) {
                reqBody.put("emails", emails);
            }
            if (mobiles != null && mobiles.length > 0) {
                reqBody.put("mobiles", mobiles);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
            
            Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class).getBody();
            
            if (resp != null && resp.get("code").equals(0)) {
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data != null) {
                    java.util.List<Map<String, Object>> userList = (java.util.List<Map<String, Object>>) data.get("user_list");
                    if (userList != null && !userList.isEmpty()) {
                        return (String) userList.get(0).get("user_id");
                    }
                }
            } else {
                System.err.println("获取用户open_id失败: " + resp.get("msg"));
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("通过手机号或邮箱获取用户open_id异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取绑定的飞书用户信息
     * @param userId 系统用户ID
     * @return 飞书用户信息
     */
    public Map<String, Object> getLarkUserInfo(Long userId) {
        try {
            // 获取用户的飞书open_id
            UserInfo userInfo = userInfoRepository.findById(userId).orElse(null);
            if (userInfo == null || userInfo.getFeishuUserId() == null) {
                System.err.println("用户未绑定飞书账号");
                return null;
            }
            
            // 获取应用access_token
            String accessToken = getAppAccessToken();
            if (accessToken == null) {
                System.err.println("获取应用access_token失败");
                return null;
            }
            
            RestTemplate restTemplate = new RestTemplate();
            
            // 调用飞书API获取用户信息
            String url = "https://open.feishu.cn/open-apis/contact/v3/users/" + userInfo.getFeishuUserId() + "?user_id_type=open_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody();
            
            if (resp != null && resp.get("code").equals(0)) {
                return (Map<String, Object>) resp.get("data");
            } else {
                System.err.println("获取飞书用户信息失败: " + resp.get("msg"));
                return null;
            }
        } catch (Exception e) {
            System.err.println("获取飞书用户信息异常: " + e.getMessage());
            return null;
        }
    }
}