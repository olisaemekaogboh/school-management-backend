// src/main/java/com/inkFront/schoolManagement/service/FileStorageService.java
package com.inkFront.schoolManagement.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    byte[] loadFile(String fileName);
    boolean deleteFile(String fileName);
    String getFileUrl(String fileName);
}