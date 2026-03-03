// src/main/java/com/inkFront/schoolManagement/dto/auth/RefreshTokenRequest.java
package com.inkFront.schoolManagement.dto.auth;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}