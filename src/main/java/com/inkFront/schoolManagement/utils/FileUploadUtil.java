// src/main/java/com/inkFront/schoolManagement/util/FileUploadUtil.java
package com.inkFront.schoolManagement.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@Slf4j
public class FileUploadUtil {

    @Value("${upload.path.students:./uploads/students/}")
    private String studentUploadPath;

    @Value("${upload.path.teachers:./uploads/teachers/}")
    private String teacherUploadPath;

    @Value("${upload.path.profile:./uploads/profiles/}")
    private String profileUploadPath;

    @Value("${upload.path.documents:./uploads/documents/}")
    private String documentUploadPath;

    /**
     * Save a file to the specified directory
     * @param basePath The base directory path
     * @param prefix File name prefix
     * @param file The multipart file to save
     * @return The generated filename
     * @throws IOException If file operations fail
     */
    public String saveFile(String basePath, String prefix, MultipartFile file) throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(basePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = prefix + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File saved: {}", filePath);

        return filename;
    }

    /**
     * Save a student profile picture
     */
    public String saveStudentProfile(String studentId, MultipartFile file) throws IOException {
        return saveFile(studentUploadPath, "student_" + studentId, file);
    }

    /**
     * Save a teacher profile picture
     */
    public String saveTeacherProfile(String teacherId, MultipartFile file) throws IOException {
        return saveFile(teacherUploadPath, "teacher_" + teacherId, file);
    }

    /**
     * Save a user profile picture
     */
    public String saveUserProfile(String username, MultipartFile file) throws IOException {
        return saveFile(profileUploadPath, "user_" + username, file);
    }

    /**
     * Delete a file
     * @param basePath The base directory path
     * @param filename The filename to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteFile(String basePath, String filename) {
        try {
            Path filePath = Paths.get(basePath).resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filename, e);
            return false;
        }
    }

    /**
     * Get file as bytes
     */
    public byte[] getFile(String basePath, String filename) throws IOException {
        Path filePath = Paths.get(basePath).resolve(filename);
        return Files.readAllBytes(filePath);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String basePath, String filename) {
        Path filePath = Paths.get(basePath).resolve(filename);
        return Files.exists(filePath);
    }

    /**
     * Validate file type
     */
    public boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif")
        );
    }

    /**
     * Validate file size (max 5MB)
     */
    public boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= 5 * 1024 * 1024; // 5MB
    }

    /**
     * Get file extension
     */
    public String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Generate a unique filename
     */
    public String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + extension;
    }
}