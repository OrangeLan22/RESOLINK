package com.orangelan.resolinkserver.config;

import com.orangelan.resolinkserver.entity.Appointment;
import com.orangelan.resolinkserver.service.AppointmentNotificationService;
import com.orangelan.resolinkserver.util.SpringContextHolder;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.stereotype.Component;

@Component
public class AppointmentListener {

    /**
     * 当预约记录保存后触发
     * 注：已在 AppointmentController 中手动添加通知任务，此处不再重复处理
     */
    // @PostPersist
    // public void postPersist(Appointment appointment) {
    //     // 添加预约通知任务
    //     AppointmentNotificationService appointmentNotificationService = SpringContextHolder.getBean(AppointmentNotificationService.class);
    //     if (appointmentNotificationService != null) {
    //         appointmentNotificationService.addAppointmentNotification(appointment);
    //     }
    // }

    /**
     * 当预约记录更新后触发
     */
    @PostUpdate
    public void postUpdate(Appointment appointment) {
        // 审批操作（approval字段变化）不触发通知任务更新
        // 只有状态(status)变化时才更新通知任务
        if (appointment.getStatus() != null && appointment.getStatus().equals("-1")) {
            // 取消预约时移除通知任务
            AppointmentNotificationService appointmentNotificationService = SpringContextHolder.getBean(AppointmentNotificationService.class);
            if (appointmentNotificationService != null) {
                appointmentNotificationService.removeAppointmentNotification(appointment.getId());
            }
        }
    }
}