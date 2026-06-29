package com.orangelan.resolinkserver.config;

import com.orangelan.resolinkserver.service.AppointmentNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationInitializer implements ApplicationRunner {

    @Autowired
    private AppointmentNotificationService appointmentNotificationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 初始化预约通知任务
        appointmentNotificationService.init();
    }
}