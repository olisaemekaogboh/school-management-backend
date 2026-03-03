// src/main/java/com/inkFront/schoolManagement/dto/CompleteRegistrationDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationDTO {
    private String token;
    private String username;
    private String password;
}