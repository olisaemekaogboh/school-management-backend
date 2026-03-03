// src/main/java/com/inkFront/schoolManagement/service/IMPL/FileStorageServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.FileStorageException;
import com.inkFront.schoolManagement.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private Path fileStorageLocation;

    @Override
    public String storeFile(MultipartFile file) {
        log.info("Storing file: {}", file.getOriginalFilename());

        try {
            if (fileStorageLocation == null) {
                fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(fileStorageLocation);
            }

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            if (fileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + fileName);
            }

            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", fileName);
            return fileName;
        } catch (IOException ex) {
            log.error("Could not store file: {}", ex.getMessage());
            throw new FileStorageException("Could not store file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] loadFile(String fileName) {
        log.info("Loading file: {}", fileName);

        try {
            if (fileStorageLocation == null) {
                fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            }

            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            log.error("Could not load file: {}", ex.getMessage());
            throw new FileStorageException("Could not load file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        log.info("Deleting file: {}", fileName);

        try {
            if (fileStorageLocation == null) {
                fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            }

            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", ex.getMessage());
            throw new FileStorageException("Could not delete file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        return baseUrl + "/uploads/" + fileName;
    }
}