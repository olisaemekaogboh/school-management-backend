package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransportStudentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String parentName;
    private String parentPhone;
}