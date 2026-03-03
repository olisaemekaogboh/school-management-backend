// src/main/java/com/inkFront/schoolManagement/exception/BusinessException.java
package com.inkFront.schoolManagement.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}