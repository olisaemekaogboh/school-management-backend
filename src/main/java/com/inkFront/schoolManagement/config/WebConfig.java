// src/main/java/com/inkFront/schoolManagement/config/WebConfig.java
package com.inkFront.schoolManagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Absolute path to the uploads directory
        String projectPath = System.getProperty("user.dir");
        String uploadPath = projectPath + "/uploads/";

        System.out.println("========== STATIC RESOURCE CONFIGURATION ==========");
        System.out.println("Project path: " + projectPath);
        System.out.println("Upload path: " + uploadPath);
        System.out.println("Serving uploads from: file:" + uploadPath);
        System.out.println("URL pattern: /uploads/**");
        System.out.println("==================================================");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}