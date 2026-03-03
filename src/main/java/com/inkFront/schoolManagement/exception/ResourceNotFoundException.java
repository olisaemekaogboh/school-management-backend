// src/main/java/com/inkFront/schoolManagement/exception/ResourceNotFoundException.java
package com.inkFront.schoolManagement.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}