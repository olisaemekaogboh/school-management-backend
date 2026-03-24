package com.inkFront.schoolManagement.security;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TeacherSubject;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessControlService {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;

    public void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Only admin can perform this action");
        }
    }

    public void requireTeacherOrAdmin(User user) {
        if (!(isAdmin(user) || isTeacher(user))) {
            throw new AccessDeniedException("Only admin or teacher can perform this action");
        }
    }

    public void requireStudentAccess(User user, Long studentId) {
        if (!canAccessStudent(user, studentId)) {
            throw new AccessDeniedException("You are not allowed to access this student's record");
        }
    }

    public void requireStudentResultAccess(User user, Long studentId) {
        if (!canViewStudentResult(user, studentId)) {
            throw new AccessDeniedException("You are not allowed to view this student's result");
        }
    }

    public void requireStudentResultModification(User user, Long studentId) {
        if (!canModifyStudentResult(user, studentId, null)) {
            throw new AccessDeniedException("You are not allowed to modify this student's result");
        }
    }

    public void requireStudentResultModification(User user, Long studentId, Long subjectId) {
        if (!canModifyStudentResult(user, studentId, subjectId)) {
            throw new AccessDeniedException(
                    "You are not allowed to modify this student's result. Only admin, the form teacher, or the assigned subject teacher for this class arm can do this."
            );
        }
    }

    public void requireAttendanceAccess(User user, Long studentId) {
        if (!canViewStudentAttendance(user, studentId)) {
            throw new AccessDeniedException("You are not allowed to view this student's attendance");
        }
    }

    public void requireAttendanceMarking(User user, Long studentId) {
        if (!canMarkAttendance(user, studentId)) {
            throw new AccessDeniedException("You are not allowed to mark attendance for this student");
        }
    }

    public void requireFeeAccess(User user, Long studentId) {
        if (!(isAdmin(user) || isOwnerStudent(user, studentId) || isParentOfStudent(user, studentId))) {
            throw new AccessDeniedException("You are not allowed to access this student's fee record");
        }
    }

    public void requireClassTeacherOrAdmin(User user, String className, String arm) {
        if (isAdmin(user)) {
            return;
        }

        if (!isTeacher(user)) {
            throw new AccessDeniedException("Only admin or assigned teacher can access this class");
        }

        if (user.getTeacher() == null) {
            throw new AccessDeniedException("Teacher account required");
        }

        if (isBlank(className) || isBlank(arm)) {
            throw new AccessDeniedException("Teachers can only access an assigned class arm");
        }

        SchoolClass schoolClass = classRepository
                .findByClassNameAndArmNormalized(className, arm)
                .orElseThrow(() -> new RuntimeException("Class not found: " + className + " " + arm));

        if (schoolClass.getClassTeacher() == null) {
            throw new AccessDeniedException("This class arm has no assigned form teacher");
        }

        if (!schoolClass.getClassTeacher().getId().equals(user.getTeacher().getId())) {
            throw new AccessDeniedException("You can only access your assigned class arm");
        }
    }

    public void requireResultClassAccess(User user, String className, String arm) {
        if (isAdmin(user)) {
            return;
        }

        if (!isTeacher(user) || user.getTeacher() == null) {
            throw new AccessDeniedException("Only admin or assigned teacher can access these results");
        }

        if (isBlank(className) || isBlank(arm)) {
            throw new AccessDeniedException("Teachers can only access an assigned class arm");
        }

        boolean formTeacherMatch = isFormTeacherOfClass(user, className, arm);

        log.info(
                "Result class access check => teacherId={}, class='{}', arm='{}', formTeacherMatch={}",
                user.getTeacher().getId(),
                className,
                arm,
                formTeacherMatch
        );

        if (!formTeacherMatch) {
            throw new AccessDeniedException("You can only access results for your assigned class arm");
        }
    }

    public boolean canAccessStudent(User user, Long studentId) {
        return isAdmin(user)
                || isOwnerStudent(user, studentId)
                || isParentOfStudent(user, studentId)
                || isFormTeacherOfStudent(user, studentId);
    }

    public boolean canViewStudentResult(User user, Long studentId) {
        log.info("Checking result access for userId={}, role={}, studentId={}",
                user != null ? user.getId() : null,
                user != null ? user.getRole() : null,
                studentId);

        if (isAdmin(user) || isOwnerStudent(user, studentId) || isParentOfStudent(user, studentId)) {
            log.info("Access granted by admin/owner/parent rule");
            return true;
        }

        Student student = findStudent(studentId);
        if (student == null) {
            log.warn("Access denied: student not found");
            return false;
        }

        log.info("Student scope => class='{}', arm='{}'",
                student.getStudentClass(), student.getClassArm());

        boolean formTeacher = isFormTeacherOfStudent(user, student);

        log.info("Result access check => formTeacher={}", formTeacher);

        return formTeacher;
    }

    public boolean canModifyStudentResult(User user, Long studentId) {
        return canModifyStudentResult(user, studentId, null);
    }

    public boolean canModifyStudentResult(User user, Long studentId, Long subjectId) {
        if (isAdmin(user)) {
            log.info("Result modification allowed: admin userId={}", user != null ? user.getId() : null);
            return true;
        }

        Student student = findStudent(studentId);
        if (student == null) {
            log.warn("Result modification denied: student not found => studentId={}", studentId);
            return false;
        }

        if (!isTeacher(user) || user.getTeacher() == null) {
            log.warn("Result modification denied: user is not a teacher => userId={}, role={}",
                    user != null ? user.getId() : null,
                    user != null ? user.getRole() : null);
            return false;
        }

        boolean formTeacher = isFormTeacherOfStudent(user, student);
        boolean subjectTeacher = isTeacherAssignedToStudentSubject(user, student, subjectId);

        log.info("Result modification decision => teacherId={}, studentId={}, studentClass={}, studentArm={}, subjectId={}, formTeacher={}, subjectTeacher={}",
                user.getTeacher().getId(),
                studentId,
                student.getStudentClass(),
                student.getClassArm(),
                subjectId,
                formTeacher,
                subjectTeacher
        );

        return formTeacher || subjectTeacher;
    }

    public boolean canViewStudentAttendance(User user, Long studentId) {
        return isAdmin(user)
                || isOwnerStudent(user, studentId)
                || isParentOfStudent(user, studentId)
                || isFormTeacherOfStudent(user, studentId);
    }

    public boolean canMarkAttendance(User user, Long studentId) {
        return isAdmin(user) || isFormTeacherOfStudent(user, studentId);
    }

    public boolean canViewStudentFees(User user, Long studentId) {
        return isAdmin(user)
                || isOwnerStudent(user, studentId)
                || isParentOfStudent(user, studentId);
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == User.Role.ADMIN;
    }

    public boolean isTeacher(User user) {
        return user != null && user.getRole() == User.Role.TEACHER;
    }

    public boolean isStudent(User user) {
        return user != null && user.getRole() == User.Role.STUDENT;
    }

    public boolean isParent(User user) {
        return user != null && user.getRole() == User.Role.PARENT;
    }

    public boolean isOwnerStudent(User user, Long studentId) {
        return isStudent(user)
                && user.getStudent() != null
                && user.getStudent().getId().equals(studentId);
    }

    public boolean isParentOfStudent(User user, Long studentId) {
        if (!isParent(user) || user.getParent() == null) {
            return false;
        }

        Parent parent = user.getParent();

        return studentRepository.findById(studentId)
                .map(student -> student.getParent() != null && student.getParent().getId().equals(parent.getId()))
                .orElse(false);
    }

    public boolean isFormTeacherOfStudent(User user, Long studentId) {
        Student student = findStudent(studentId);
        return student != null && isFormTeacherOfStudent(user, student);
    }

    private boolean isFormTeacherOfStudent(User user, Student student) {
        if (!isTeacher(user) || user.getTeacher() == null || student == null) {
            log.warn("Form teacher check failed: user is not teacher or teacher profile is null");
            return false;
        }

        if (!hasStudentClassScope(student)) {
            log.warn("Form teacher check failed: student has no valid class scope");
            return false;
        }

        Optional<SchoolClass> schoolClass = classRepository.findByClassNameAndArmNormalized(
                student.getStudentClass(),
                student.getClassArm()
        );

        if (schoolClass.isEmpty()) {
            log.warn("Form teacher check failed: no SchoolClass found for class='{}', arm='{}'",
                    student.getStudentClass(), student.getClassArm());
            return false;
        }

        if (schoolClass.get().getClassTeacher() == null) {
            log.warn("Form teacher check failed: class has no class teacher");
            return false;
        }

        boolean match = schoolClass.get().getClassTeacher().getId().equals(user.getTeacher().getId());

        log.info("Form teacher check => classTeacherId={}, currentTeacherId={}, match={}",
                schoolClass.get().getClassTeacher().getId(),
                user.getTeacher().getId(),
                match);

        return match;
    }

    private boolean isTeacherAssignedToStudentSubject(User user, Student student, Long subjectId) {
        if (!isTeacher(user) || user.getTeacher() == null || student == null || !hasStudentClassScope(student)) {
            return false;
        }

        if (subjectId == null) {
            log.warn("Subject assignment check denied: subjectId is null");
            return false;
        }

        List<TeacherSubject> assignments =
                teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(user.getTeacher().getId());

        log.info("Checking teacher subject assignments => teacherId={}, subjectId={}, studentClass={}, studentArm={}, assignmentsCount={}",
                user.getTeacher().getId(),
                subjectId,
                student.getStudentClass(),
                student.getClassArm(),
                assignments.size()
        );

        for (TeacherSubject assignment : assignments) {
            Long assignedSubjectId = assignment.getSubject() != null ? assignment.getSubject().getId() : null;
            boolean sameScope = sameClassScope(
                    student.getStudentClass(),
                    student.getClassArm(),
                    assignment.getClassName(),
                    assignment.getClassArm()
            );
            boolean subjectMatch = assignedSubjectId != null && assignedSubjectId.equals(subjectId);

            log.info("Assignment candidate => className={}, classArm={}, assignedSubjectId={}, sameScope={}, subjectMatch={}",
                    assignment.getClassName(),
                    assignment.getClassArm(),
                    assignedSubjectId,
                    sameScope,
                    subjectMatch
            );

            if (sameScope && subjectMatch) {
                return true;
            }
        }

        return false;
    }

    private boolean isFormTeacherOfClass(User user, String className, String arm) {
        if (!isTeacher(user) || user.getTeacher() == null || isBlank(className) || isBlank(arm)) {
            return false;
        }

        Optional<SchoolClass> schoolClass = classRepository.findByClassNameAndArmNormalized(className, arm);
        return schoolClass.isPresent()
                && schoolClass.get().getClassTeacher() != null
                && schoolClass.get().getClassTeacher().getId().equals(user.getTeacher().getId());
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId).orElse(null);
    }

    private boolean hasStudentClassScope(Student student) {
        return !isBlank(student.getStudentClass()) && !isBlank(student.getClassArm());
    }

    private boolean sameClassScope(String className1, String arm1, String className2, String arm2) {
        return normalizeCompact(className1).equals(normalizeCompact(className2))
                && normalizeCompact(arm1).equals(normalizeCompact(arm2));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeCompact(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}