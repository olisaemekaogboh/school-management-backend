package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermResultResponseDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;

    private String session;
    private String term;

    private double totalScore;
    private double average;
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    private int totalSchoolDays;
    private int daysPresent;
    private int daysAbsent;
    private double attendancePercentage;

    private String classTeacherComment;
    private String principalComment;

    private List<AssessmentItemDTO> characterTraits;
    private List<AssessmentItemDTO> psychomotorTraits;
    private LocalDate nextTermBegins;

    private boolean printable;
    private String printLockMessage;
    private ResultVisibilityStatus visibilityStatus;
    private String visibilityMessage;
    private LocalDateTime publishedAt;
    private String publishedByName;

    private Boolean classTeacherSigned;
    private Boolean adminSigned;
    private Boolean completed;
    private LocalDateTime classTeacherSignedAt;
    private LocalDateTime adminSignedAt;
    private String classTeacherSignatureUrl;
    private String adminSignatureUrl;

    private List<ResultResponseDTO> subjectResults;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TermResultResponseDTO fromTermResult(
            TermResult termResult,
            List<AssessmentItemDTO> characterTraits,
            List<AssessmentItemDTO> psychomotorTraits
    ) {
        if (termResult == null) {
            return null;
        }

        Student student = termResult.getStudent();

        List<ResultResponseDTO> subjectResults = termResult.getSubjectResults() != null
                ? termResult.getSubjectResults().stream()
                .map(ResultResponseDTO::fromResult)
                .collect(Collectors.toList())
                : List.of();

        String studentFullName = student == null
                ? null
                : (
                safe(student.getFirstName()) + " " +
                        safe(student.getMiddleName()) + " " +
                        safe(student.getLastName())
        ).replaceAll("\\s+", " ").trim();

        return TermResultResponseDTO.builder()
                .id(termResult.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(studentFullName)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .studentClass(student != null ? student.getStudentClass() : null)
                .classArm(student != null ? student.getClassArm() : null)
                .session(termResult.getSession())
                .term(termResult.getTerm() != null ? termResult.getTerm().name() : null)
                .totalScore(termResult.getTotalScore())
                .average(termResult.getAverage())
                .positionInClass(termResult.getPositionInClass())
                .positionInArm(termResult.getPositionInArm())
                .positionInSchool(termResult.getPositionInSchool())
                .totalSchoolDays(termResult.getTotalSchoolDays())
                .daysPresent(termResult.getDaysPresent())
                .daysAbsent(termResult.getDaysAbsent())
                .attendancePercentage(termResult.getAttendancePercentage())
                .classTeacherComment(termResult.getClassTeacherComment())
                .principalComment(termResult.getPrincipalComment())
                .characterTraits(characterTraits)
                .psychomotorTraits(psychomotorTraits)
                .nextTermBegins(termResult.getNextTermBegins())
                .printable(termResult.isPrintable())
                .printLockMessage(termResult.getPrintLockMessage())
                .visibilityStatus(termResult.getVisibilityStatus())
                .visibilityMessage(termResult.getVisibilityMessage())
                .publishedAt(termResult.getPublishedAt())
                .publishedByName(termResult.getPublishedByName())
                .classTeacherSigned(termResult.isClassTeacherSigned())
                .adminSigned(termResult.isAdminSigned())
                .completed(termResult.isCompleted())
                .classTeacherSignedAt(termResult.getClassTeacherSignedAt())
                .adminSignedAt(termResult.getAdminSignedAt())
                .classTeacherSignatureUrl(termResult.getClassTeacherSignatureUrl())
                .adminSignatureUrl(termResult.getAdminSignatureUrl())
                .subjectResults(subjectResults)
                .createdAt(termResult.getCreatedAt())
                .updatedAt(termResult.getUpdatedAt())
                .build();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}