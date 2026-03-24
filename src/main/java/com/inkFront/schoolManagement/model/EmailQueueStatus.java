// src/main/java/com/inkFront/schoolManagement/model/EmailQueueStatus.java
package com.inkFront.schoolManagement.model;

public enum EmailQueueStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    RETRYING
}