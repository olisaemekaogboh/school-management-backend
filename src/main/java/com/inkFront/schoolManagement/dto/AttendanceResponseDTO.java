package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentClass;
    private String classArm;
    private LocalDate date;
    private String session;
    private String term;
    private String status;

    public static AttendanceResponseDTO fromEntity(Attendance attendance) {
        return AttendanceResponseDTO.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent() != null ? attendance.getStudent().getId() : null)
                .studentName(attendance.getStudent() != null
                        ? (attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName()).trim()
                        : null)
                .studentClass(attendance.getStudent() != null ? attendance.getStudent().getStudentClass() : null)
                .classArm(attendance.getStudent() != null ? attendance.getStudent().getClassArm() : null)
                .date(attendance.getDate())
                .session(attendance.getSession())
                .term(attendance.getTerm() != null ? attendance.getTerm().name() : null)
                .status(attendance.getStatus() != null ? attendance.getStatus().name() : null)
                .build();
    }
}