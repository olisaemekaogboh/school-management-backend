// src/main/java/com/inkFront/schoolManagement/dto/ReminderResult.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResult {
    private Integer totalSent;
    private Integer successful;
    private Integer failed;
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> successfulNumbers = new ArrayList<>();
    private List<String> failedNumbers = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String message;
}