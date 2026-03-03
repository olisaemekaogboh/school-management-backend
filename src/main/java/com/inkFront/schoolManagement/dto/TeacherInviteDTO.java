// src/main/java/com/inkFront/schoolManagement/dto/TeacherInviteDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInviteDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}