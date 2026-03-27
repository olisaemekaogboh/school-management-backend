package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.FileStorageException;
import com.inkFront.schoolManagement.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
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

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        try {
            fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(fileStorageLocation);
            log.info("File storage initialized at {}", fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        log.info("Storing file: {}", file.getOriginalFilename());

        try {
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";

            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + fileExtension;

            if (fileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + fileName);
            }

            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", fileName);

            return getFileUrl(fileName);
        } catch (IOException ex) {
            log.error("Could not store file: {}", ex.getMessage());
            throw new FileStorageException("Could not store file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] loadFile(String fileName) {
        String cleanFileName = extractFileName(fileName);
        log.info("Loading file: {}", cleanFileName);

        try {
            Path filePath = fileStorageLocation.resolve(cleanFileName).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            log.error("Could not load file: {}", ex.getMessage());
            throw new FileStorageException("Could not load file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        String cleanFileName = extractFileName(fileName);
        log.info("Deleting file: {}", cleanFileName);

        try {
            Path filePath = fileStorageLocation.resolve(cleanFileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", ex.getMessage());
            throw new FileStorageException("Could not delete file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return normalizedBaseUrl + "/uploads/" + fileName;
    }

    private String extractFileName(String value) {
        if (value == null || value.isBlank()) {
            throw new FileStorageException("File name cannot be null or blank");
        }

        String normalized = value.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf("/");

        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}