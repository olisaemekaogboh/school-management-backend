// src/main/java/com/inkFront/schoolManagement/dto/PublicResponse.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicResponse<T> {
    private boolean success;
    private String message;
    private T data;
}