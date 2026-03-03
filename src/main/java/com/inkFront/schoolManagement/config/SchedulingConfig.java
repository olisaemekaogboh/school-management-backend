// src/main/java/com/inkFront/schoolManagement/config/SchedulingConfig.java
package com.inkFront.schoolManagement.config;

import com.inkFront.schoolManagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Autowired
    private StudentService studentService;

    // Run at 2 AM on September 1st every year (adjust date as needed)
    @Scheduled(cron = "0 0 2 1 9 ?")
    public void automaticPromotion() {
        studentService.promoteAllStudents();
    }

    // Alternative: Run at 2 AM on the first day of every month
    // @Scheduled(cron = "0 0 2 1 * ?")
    // public void monthlyCheck() {
    //     // Check if it's end of session and promote
    // }
}