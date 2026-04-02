package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "baseUrl", "http://localhost:8080");
        fileStorageService.init();
    }

    @Test
    void init_ShouldCreateDirectory() {
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void storeFile_ShouldStoreAndReturnUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String url = fileStorageService.storeFile(file);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/uploads/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void storeFile_WithEmptyFile_ShouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(FileStorageException.class, () -> {
            fileStorageService.storeFile(emptyFile);
        });
    }

    @Test
    void storeFile_WithNullFile_ShouldThrowException() {
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.storeFile(null);
        });
    }

    @Test
    void storeFile_WithNoExtension_ShouldGenerateFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test",
                "image/jpeg",
                "test content".getBytes()
        );

        String url = fileStorageService.storeFile(file);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/uploads/"));
        assertFalse(url.endsWith(".jpg"));
    }

    @Test
    void loadFile_ShouldReturnFileBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        String url = fileStorageService.storeFile(file);
        String fileName = url.substring(url.lastIndexOf("/") + 1);

        byte[] loadedBytes = fileStorageService.loadFile(fileName);

        assertNotNull(loadedBytes);
        assertArrayEquals("test content".getBytes(), loadedBytes);
    }

    @Test
    void loadFile_WithNonExistentFile_ShouldThrowException() {
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.loadFile("nonexistent.jpg");
        });
    }

    @Test
    void loadFile_WithFullUrl_ShouldExtractFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        String url = fileStorageService.storeFile(file);

        byte[] loadedBytes = fileStorageService.loadFile(url);

        assertNotNull(loadedBytes);
        assertArrayEquals("test content".getBytes(), loadedBytes);
    }

    @Test
    void deleteFile_ShouldDeleteFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        String url = fileStorageService.storeFile(file);
        String fileName = url.substring(url.lastIndexOf("/") + 1);

        boolean deleted = fileStorageService.deleteFile(fileName);

        assertTrue(deleted);
    }

    @Test
    void deleteFile_WithNonExistentFile_ShouldReturnFalse() {
        boolean deleted = fileStorageService.deleteFile("nonexistent.jpg");

        assertFalse(deleted);
    }

    @Test
    void deleteFile_WithFullUrl_ShouldExtractAndDelete() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        String url = fileStorageService.storeFile(file);

        boolean deleted = fileStorageService.deleteFile(url);

        assertTrue(deleted);
    }

    @Test
    void getFileUrl_ShouldReturnFullUrl() {
        String url = fileStorageService.getFileUrl("test.jpg");

        assertEquals("http://localhost:8080/uploads/test.jpg", url);
    }

    @Test
    void getFileUrl_WithBaseUrlTrailingSlash_ShouldNormalize() {
        ReflectionTestUtils.setField(fileStorageService, "baseUrl", "http://localhost:8080/");
        String url = fileStorageService.getFileUrl("test.jpg");

        assertEquals("http://localhost:8080/uploads/test.jpg", url);
    }

    @Test
    void extractFileName_ShouldExtractFromFullPath() {
        String fileName = ReflectionTestUtils.invokeMethod(fileStorageService,
                "extractFileName", "http://localhost:8080/uploads/test.jpg");

        assertEquals("test.jpg", fileName);
    }

    @Test
    void extractFileName_WithWindowsPath_ShouldExtract() {
        String fileName = ReflectionTestUtils.invokeMethod(fileStorageService,
                "extractFileName", "C:\\uploads\\test.jpg");

        assertEquals("test.jpg", fileName);
    }

    @Test
    void extractFileName_WithNull_ShouldThrowException() {
        assertThrows(FileStorageException.class, () -> {
            ReflectionTestUtils.invokeMethod(fileStorageService, "extractFileName", (Object) null);
        });
    }

    @Test
    void extractFileName_WithBlank_ShouldThrowException() {
        assertThrows(FileStorageException.class, () -> {
            ReflectionTestUtils.invokeMethod(fileStorageService, "extractFileName", "");
        });
    }
}
