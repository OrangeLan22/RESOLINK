package com.orangelan.resolinkserver.service;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.entity.Authority;
import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.entity.SpaceResource;
import com.orangelan.resolinkserver.entity.PhysicalResource;
import com.orangelan.resolinkserver.repository.AppointmentRepository;
import com.orangelan.resolinkserver.repository.AuthorityRepository;
import com.orangelan.resolinkserver.repository.UserInfoRepository;
import com.orangelan.resolinkserver.repository.SpaceResourceRepository;
import com.orangelan.resolinkserver.repository.PhysicalResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppointmentNotificationService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private AuthorityRepository authorityRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private SpaceResourceRepository spaceResourceRepository;
    
    @Autowired
    private PhysicalResourceRepository physicalResourceRepository;
    
    @Autowired
    private LarkMessageService larkMessageService;
    
    // 消息队列，存储需要发送的通知任务
    private final Map<Long, NotificationTask> notificationTasks = new ConcurrentHashMap<>();
    
    // 日期格式
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    /**
     * 格式化时间戳为易读的时间字符串
     * @param timestamp 时间戳（秒）
     * @return 格式化后的时间字符串
     */
    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp * 1000);
        return datetimeFormat.format(date);
    }
    
    /**
     * 项目启动时初始化，获取需要通知的预约记录
     */
    public void init() {
        // 获取所有预约记录
        List<Appointment> appointments = appointmentRepository.findAll();
        long currentTimestamp = System.currentTimeMillis() / 1000;
        
        for (Appointment appointment : appointments) {
            try {
                // 解析开始时间和结束时间（秒级时间戳）
                long startTimestamp = Long.parseLong(appointment.getStartTime());
                long endTimestamp = Long.parseLong(appointment.getEndTime());
                
                // 如果当前时间已经在结束时间之后，则不需要处理
                if (currentTimestamp > endTimestamp) {
                    continue;
                }
                
                // 格式化时间显示
                String startTimeStr = formatTimestamp(startTimestamp);
                String endTimeStr = formatTimestamp(endTimestamp);
                
                // 获取资源位置
                String location = "";
                if (appointment.getType() != null) {
                    if (appointment.getType().equals("space")) {
                        SpaceResource spaceResource = spaceResourceRepository.findById(appointment.getResId()).orElse(null);
                        if (spaceResource != null) {
                            location = spaceResource.getLocation();
                        }
                    } else if (appointment.getType().equals("physical")) {
                        PhysicalResource physicalResource = physicalResourceRepository.findById(appointment.getResId()).orElse(null);
                        if (physicalResource != null) {
                            location = physicalResource.getLocation();
                        }
                    }
                }
                
                // 获取飞书用户ID
                String feishuUserId = null;
                if (appointment.getEmpId() != null) {
                    UserInfo userInfo = userInfoRepository.findByEmpId(appointment.getEmpId());
                    if (userInfo != null) {
                        feishuUserId = userInfo.getFeishuUserId();
                    }
                }
                
                // 如果当前时间在开始时间之前，则需要发送开始提醒和结束提醒
                if (currentTimestamp < startTimestamp) {
                    // 添加开始提醒任务（提前30分钟）
                    long reminderTimestamp = startTimestamp - 30 * 60;
                    if (reminderTimestamp > currentTimestamp && feishuUserId != null) {
                        notificationTasks.put(appointment.getId() * 2, new NotificationTask(
                                appointment.getId(),
                                reminderTimestamp,
                                NotificationType.START_REMINDER,
                                feishuUserId,
                                appointment.getName(),
                                appointment.getResName(),
                                appointment.getResId(),
                                location,
                                startTimeStr,
                                endTimeStr
                        ));
                    }
                    
                    // 添加结束提醒任务
                    notificationTasks.put(appointment.getId() * 2 + 1, new NotificationTask(
                            appointment.getId(),
                            endTimestamp,
                            NotificationType.END_REMINDER,
                            null, // 设备检查员需要根据资源ID查询
                            appointment.getName(),
                            appointment.getResName(),
                            appointment.getResId(),
                            location,
                            startTimeStr,
                            endTimeStr
                    ));
                } else if (currentTimestamp >= startTimestamp && currentTimestamp < endTimestamp) {
                    // 如果当前时间在开始时间和结束时间之间，则只需要发送结束提醒
                    notificationTasks.put(appointment.getId() * 2 + 1, new NotificationTask(
                            appointment.getId(),
                            endTimestamp,
                            NotificationType.END_REMINDER,
                            null, // 设备检查员需要根据资源ID查询
                            appointment.getName(),
                            appointment.getResName(),
                            appointment.getResId(),
                            location,
                            startTimeStr,
                            endTimeStr
                    ));
                }
            } catch (NumberFormatException e) {
                System.err.println("解析预约时间戳失败: " + e.getMessage());
            }
        }
        
        System.out.println("初始化预约通知任务完成，共添加 " + notificationTasks.size() + " 个任务");
    }
    
    /**
     * 定时检查并发送通知
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkAndSendNotifications() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        List<Long> tasksToRemove = new ArrayList<>();
        
        for (Map.Entry<Long, NotificationTask> entry : notificationTasks.entrySet()) {
            NotificationTask task = entry.getValue();
            
            // 如果到了发送时间
            if (currentTimestamp >= task.getTimestamp()) {
                // 发送通知
                sendNotification(task);
                // 标记为需要移除
                tasksToRemove.add(entry.getKey());
            }
        }
        
        // 移除已处理的任务
        for (Long taskId : tasksToRemove) {
            notificationTasks.remove(taskId);
        }
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(NotificationTask task) {
        if (task.getType() == NotificationType.START_REMINDER) {
            // 发送资源即将开放通知给预约人
            // 构建资源即将开始卡片消息（使用模板卡片ID）
            String cardContent = "{\n" +
                "  \"type\": \"template\",\n" +
                "  \"data\": {\n" +
                "    \"template_id\": \"AAqKSLVrnh7jS\",\n" +
                "    \"template_variable\": {\n" +
                "      \"RESO_NAME\": \"" + task.getResName() + "\",\n" +
                "      \"LOCATION\": \"" + task.getLocation() + "\",\n" +
                "      \"START_TIME\": \"" + task.getStartTime() + "\",\n" +
                "      \"END_TIME\": \"" + task.getEndTime() + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
            
            // 发送卡片消息
            boolean sendResult = larkMessageService.sendCardMessage(task.getUserId(), cardContent, "open_id");
            System.out.println("发送开始提醒通知给用户 " + task.getUserName() + "，资源 " + task.getResName() + "，结果：" + sendResult);
        } else if (task.getType() == NotificationType.END_REMINDER) {
            // 发送资源待检查消息给设备检查员
            String inspectorId = getDeviceInspector(task.getAppointmentId());
            
            // 构建待检查卡片消息（使用模板卡片ID）
            String cardContent = "{\n" +
                "  \"type\": \"template\",\n" +
                "  \"data\": {\n" +
                "    \"template_id\": \"AAqKSYg0BsbPQ\",\n" +
                "    \"template_variable\": {\n" +
                "      \"RESO_NAME\": \"" + task.getResName() + "\",\n" +
                "      \"ID\": \"" + task.getResId() + "\",\n" +
                "      \"LOCATION\": \"" + task.getLocation() + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
            
            // 发送卡片消息
            boolean sendResult = larkMessageService.sendCardMessage(inspectorId, cardContent, "open_id");
            System.out.println("发送结束提醒通知给设备检查员，资源 " + task.getResName() + "，结果：" + sendResult);
        }
    }
    
    /**
     * 获取设备检查员
     * 根据任务书要求：在权限表（authoritys）中的check字段，如果为1则代表是资源检察员，
     * 只需要将check为1的id提取出来，对照user_info表的auth_id即可。
     */
    private String getDeviceInspector(Long appointmentId) {
        try {
            // 从权限表中获取check字段为1的记录
            List<Authority> authorities = authorityRepository.findAll();
            List<Long> checkAuthIds = new ArrayList<>();
            for (Authority authority : authorities) {
                if (authority.getCheck() == 1) {
                    checkAuthIds.add(authority.getId());
                }
            }
            
            // 从user_info表中获取auth_id与这些权限ID匹配的用户
            if (!checkAuthIds.isEmpty()) {
                List<UserInfo> allUsers = userInfoRepository.findAll();
                for (UserInfo user : allUsers) {
                    if (checkAuthIds.contains(user.getAuthId()) && user.getFeishuUserId() != null && !user.getFeishuUserId().isEmpty()) {
                        return user.getFeishuUserId();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取设备检查员失败: " + e.getMessage());
        }
        // 如果没有找到设备检查员，返回一个默认值
        return "设备检查员ID";
    }
    
    /**
     * 添加预约通知任务
     */
    public void addAppointmentNotification(Appointment appointment) {
        try {
            // 检查预约ID是否为null
            if (appointment.getId() == null) {
                System.err.println("预约ID为null，无法添加通知任务");
                return;
            }
            
            // 解析开始时间和结束时间（秒级时间戳）
            long startTimestamp = Long.parseLong(appointment.getStartTime());
            long endTimestamp = Long.parseLong(appointment.getEndTime());
            long currentTimestamp = System.currentTimeMillis() / 1000;
            
            // 如果当前时间已经在结束时间之后，则不需要处理
            if (currentTimestamp > endTimestamp) {
                return;
            }
            
            // 格式化时间显示
            String startTimeStr = formatTimestamp(startTimestamp);
            String endTimeStr = formatTimestamp(endTimestamp);
            
            // 获取资源位置
            String location = "";
            if (appointment.getType() != null) {
                if (appointment.getType().equals("space")) {
                    SpaceResource spaceResource = spaceResourceRepository.findById(appointment.getResId()).orElse(null);
                    if (spaceResource != null) {
                        location = spaceResource.getLocation();
                    }
                } else if (appointment.getType().equals("physical")) {
                    PhysicalResource physicalResource = physicalResourceRepository.findById(appointment.getResId()).orElse(null);
                    if (physicalResource != null) {
                        location = physicalResource.getLocation();
                    }
                }
            }
            
            // 获取飞书用户ID
            String feishuUserId = null;
            if (appointment.getEmpId() != null) {
                UserInfo userInfo = userInfoRepository.findByEmpId(appointment.getEmpId());
                if (userInfo != null) {
                    feishuUserId = userInfo.getFeishuUserId();
                }
            }
            
            // 如果当前时间在开始时间之前，则需要发送开始提醒和结束提醒
            if (currentTimestamp < startTimestamp) {
                // 添加开始提醒任务（提前30分钟）
                long reminderTimestamp = startTimestamp - 30 * 60;
                if (reminderTimestamp > currentTimestamp && feishuUserId != null) {
                    notificationTasks.put(appointment.getId() * 2, new NotificationTask(
                            appointment.getId(),
                            reminderTimestamp,
                            NotificationType.START_REMINDER,
                            feishuUserId,
                            appointment.getName(),
                            appointment.getResName(),
                            appointment.getResId(),
                            location,
                            startTimeStr,
                            endTimeStr
                    ));
                }
                
                // 添加结束提醒任务
                notificationTasks.put(appointment.getId() * 2 + 1, new NotificationTask(
                        appointment.getId(),
                        endTimestamp,
                        NotificationType.END_REMINDER,
                        null, // 设备检查员需要根据资源ID查询
                        appointment.getName(),
                        appointment.getResName(),
                        appointment.getResId(),
                        location,
                        startTimeStr,
                        endTimeStr
                ));
            } else if (currentTimestamp >= startTimestamp && currentTimestamp < endTimestamp) {
                // 如果当前时间在开始时间和结束时间之间，则只需要发送结束提醒
                notificationTasks.put(appointment.getId() * 2 + 1, new NotificationTask(
                        appointment.getId(),
                        endTimestamp,
                        NotificationType.END_REMINDER,
                        null, // 设备检查员需要根据资源ID查询
                        appointment.getName(),
                        appointment.getResName(),
                        appointment.getResId(),
                        location,
                        startTimeStr,
                        endTimeStr
                ));
            }
            
            System.out.println("添加预约通知任务成功，预约ID: " + appointment.getId());
        } catch (NumberFormatException e) {
            System.err.println("解析预约时间戳失败: " + e.getMessage());
        }
    }
    
    /**
     * 移除预约通知任务
     */
    public void removeAppointmentNotification(Long appointmentId) {
        notificationTasks.remove(appointmentId * 2);
        notificationTasks.remove(appointmentId * 2 + 1);
        System.out.println("移除预约通知任务成功，预约ID: " + appointmentId);
    }
    
    /**
     * 通知任务类型
     */
    private enum NotificationType {
        START_REMINDER, // 开始提醒
        END_REMINDER    // 结束提醒
    }
    
    /**
     * 通知任务
     */
    private static class NotificationTask {
        private final Long appointmentId;
        private final long timestamp;
        private final NotificationType type;
        private final String userId;
        private final String userName;
        private final String resName;
        private final Long resId;
        private final String location;
        private final String startTime;
        private final String endTime;
        
        public NotificationTask(Long appointmentId, long timestamp, NotificationType type, 
                               String userId, String userName, String resName, Long resId, String location, String startTime, String endTime) {
            this.appointmentId = appointmentId;
            this.timestamp = timestamp;
            this.type = type;
            this.userId = userId;
            this.userName = userName;
            this.resName = resName;
            this.resId = resId;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public Long getAppointmentId() {
            return appointmentId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public NotificationType getType() {
            return type;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getUserName() {
            return userName;
        }
        
        public String getResName() {
            return resName;
        }
        
        public Long getResId() {
            return resId;
        }
        
        public String getLocation() {
            return location;
        }
        
        public String getStartTime() {
            return startTime;
        }
        
        public String getEndTime() {
            return endTime;
        }
    }
}