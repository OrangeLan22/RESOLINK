package com.orangelan.resolinkserver.service;

import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LarkMessageService {
    
    @Autowired
    private Client larkClient;
    
    /**
     * 向飞书用户发送文本消息
     * @param userId 飞书用户ID
     * @param content 消息内容
     * @param receiveIdType 接收ID类型：user_id、open_id、email、chat_id
     * @return 是否发送成功
     */
    public boolean sendTextMessage(String userId, String content, String receiveIdType) {
        try {
            // 构建消息内容
            String textContent = "{\"text\": \"" + content + "\"}";
            
            CreateMessageReqBody reqBody = new CreateMessageReqBody();
            reqBody.setReceiveId(userId);
            reqBody.setContent(textContent);
            reqBody.setMsgType("text");
            
            // 发送消息
            CreateMessageReq req = new CreateMessageReq();
            req.setReceiveIdType(receiveIdType);
            req.setCreateMessageReqBody(reqBody);
            
            CreateMessageResp resp = larkClient.im().message().create(req);
            
            // 检查响应
            if (resp.getCode() == 0) {
                return true;
            } else {
                System.err.println("发送飞书消息失败: " + resp.getMsg());
                return false;
            }
        } catch (Exception e) {
            System.err.println("发送飞书消息异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 向飞书用户发送文本消息（默认使用user_id）
     * @param userId 飞书用户ID
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendTextMessage(String userId, String content) {
        return sendTextMessage(userId, content, "user_id");
    }
    
    /**
     * 向飞书用户发送卡片消息
     * @param userId 飞书用户ID
     * @param cardContent 卡片内容（JSON格式）
     * @param receiveIdType 接收ID类型：user_id、open_id、email、chat_id
     * @return 是否发送成功
     */
    public boolean sendCardMessage(String userId, String cardContent, String receiveIdType) {
        try {
            CreateMessageReqBody reqBody = new CreateMessageReqBody();
            reqBody.setReceiveId(userId);
            reqBody.setContent(cardContent);
            reqBody.setMsgType("interactive");
            
            CreateMessageReq req = new CreateMessageReq();
            req.setReceiveIdType(receiveIdType);
            req.setCreateMessageReqBody(reqBody);
            
            CreateMessageResp resp = larkClient.im().message().create(req);
            
            if (resp.getCode() == 0) {
                return true;
            } else {
                System.err.println("发送飞书卡片消息失败: " + resp.getMsg());
                return false;
            }
        } catch (Exception e) {
            System.err.println("发送飞书卡片消息异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 向飞书用户发送卡片消息（默认使用user_id）
     * @param userId 飞书用户ID
     * @param cardContent 卡片内容（JSON格式）
     * @return 是否发送成功
     */
    public boolean sendCardMessage(String userId, String cardContent) {
        return sendCardMessage(userId, cardContent, "user_id");
    }
}