// src/main/java/com/inkFront/schoolManagement/dto/auth/LoginResponse.java
package com.inkFront.schoolManagement.dto.auth;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponse user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String phoneNumber;
        private String role;
        private String profilePictureUrl;
        private boolean active;
        private boolean emailVerified;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;

        // linked entity ids
        private Long studentId;
        private Long teacherId;
        private Long parentId;

        // flat student fields for frontend convenience
        private String admissionNumber;
        private String studentClass;
        private String classArm;
        private String status;

        // optional nested summaries
        private StudentSummary student;
        private TeacherSummary teacher;
        private ParentSummary parent;

        public static UserResponse fromUser(User user) {
            if (user == null) return null;

            Student student = user.getStudent();
            Teacher teacher = user.getTeacher();
            Parent parent = user.getParent();

            return UserResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .active(user.isActive())
                    .emailVerified(user.isEmailVerified())
                    .lastLogin(user.getLastLogin())
                    .createdAt(user.getCreatedAt())

                    .studentId(student != null ? student.getId() : null)
                    .teacherId(teacher != null ? teacher.getId() : null)
                    .parentId(parent != null ? parent.getId() : null)

                    .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                    .studentClass(student != null ? student.getStudentClass() : null)
                    .classArm(student != null ? student.getClassArm() : null)
                    .status(resolveStudentStatus(student))

                    .student(StudentSummary.from(student))
                    .teacher(TeacherSummary.from(teacher))
                    .parent(ParentSummary.from(parent))
                    .build();
        }

        private static String resolveStudentStatus(Student student) {
            if (student == null || student.getStatus() == null) {
                return null;
            }

            try {
                return student.getStatus().toString();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long id;
        private String firstName;
        private String middleName;
        private String lastName;
        private String fullName;
        private String admissionNumber;
        private String studentClass;
        private String classArm;
        private String status;
        private String profilePictureUrl;
        private String parentName;
        private String parentPhone;

        public static StudentSummary from(Student student) {
            if (student == null) return null;

            return StudentSummary.builder()
                    .id(student.getId())
                    .firstName(student.getFirstName())
                    .middleName(student.getMiddleName())
                    .lastName(student.getLastName())
                    .fullName(buildFullName(
                            student.getFirstName(),
                            student.getMiddleName(),
                            student.getLastName()
                    ))
                    .admissionNumber(student.getAdmissionNumber())
                    .studentClass(student.getStudentClass())
                    .classArm(student.getClassArm())
                    .status(student.getStatus() != null ? student.getStatus().toString() : null)
                    .profilePictureUrl(student.getProfilePictureUrl())
                    .parentName(student.getParentName())
                    .parentPhone(student.getParentPhone())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String teacherId;
        private String profilePictureUrl;

        public static TeacherSummary from(Teacher teacher) {
            if (teacher == null) return null;

            return TeacherSummary.builder()
                    .id(teacher.getId())
                    .firstName(teacher.getFirstName())
                    .lastName(teacher.getLastName())
                    .fullName(buildFullName(
                            teacher.getFirstName(),
                            null,
                            teacher.getLastName()
                    ))
                    .teacherId(resolveTeacherCode(teacher))
                    .profilePictureUrl(teacher.getProfilePictureUrl())
                    .build();
        }

        private static String resolveTeacherCode(Teacher teacher) {
            try {
                return teacher.getTeacherId();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentSummary {
        private Long id;
        private String firstName;
        private String middleName;
        private String lastName;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String relationship;
        private String profilePictureUrl;

        public static ParentSummary from(Parent parent) {
            if (parent == null) return null;

            return ParentSummary.builder()
                    .id(parent.getId())
                    .firstName(parent.getFirstName())
                    .middleName(parent.getMiddleName())
                    .lastName(parent.getLastName())
                    .fullName(buildFullName(
                            parent.getFirstName(),
                            parent.getMiddleName(),
                            parent.getLastName()
                    ))
                    .email(parent.getEmail())
                    .phoneNumber(parent.getPhoneNumber())
                    .relationship(resolveRelationship(parent))
                    .profilePictureUrl(parent.getProfilePictureUrl())
                    .build();
        }

        private static String resolveRelationship(Parent parent) {
            if (parent == null || parent.getRelationship() == null) {
                return null;
            }

            return parent.getRelationship().name();
        }
    }

    private static String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder sb = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }

        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }

        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }

        return sb.toString().trim();
    }
}