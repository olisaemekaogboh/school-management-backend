// src/main/java/com/inkFront/schoolManagement/config/WebConfig.java
package com.inkFront.schoolManagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")   // allow all endpoints including uploads
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }







        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // Get the absolute path to the uploads directory
            String projectPath = System.getProperty("user.dir");
            String uploadPath = projectPath + "/uploads/";

            System.out.println("========== STATIC RESOURCE CONFIGURATION ==========");
            System.out.println("Project path: " + projectPath);
            System.out.println("Upload path: " + uploadPath);
            System.out.println("Serving uploads from: file:" + uploadPath);
            System.out.println("URL pattern: /uploads/**");
            System.out.println("==================================================");

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadPath)
                    .setCachePeriod(3600);

    }
}