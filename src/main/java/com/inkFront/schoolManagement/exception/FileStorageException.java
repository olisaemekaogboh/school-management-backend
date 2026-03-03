// src/main/java/com/inkFront/schoolManagement/exception/FileStorageException.java
package com.inkFront.schoolManagement.exception;

public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}