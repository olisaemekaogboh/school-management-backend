package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentWardResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String admissionNumber;
    private Long classId;
    private String studentClass;
    private String classArm;
    private String profilePictureUrl;

    public static ParentWardResponseDTO fromStudent(Student student) {
        return ParentWardResponseDTO.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(
                        ((student.getFirstName() != null ? student.getFirstName() : "") + " " +
                                (student.getLastName() != null ? student.getLastName() : ""))
                                .trim()
                )
                .admissionNumber(student.getAdmissionNumber())
                .classId(student.getSchoolClass() != null ? student.getSchoolClass().getId() : null)
                .studentClass(student.getStudentClass())
                .classArm(student.getClassArm())
                .profilePictureUrl(student.getProfilePictureUrl())
                .build();
    }
}