package com.inkFront.schoolManagement.security;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TeacherSubject;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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

    public void requireStudentTermResultViewAccess(User user, TermResult termResult) {
        if (!canViewTermResult(user, termResult)) {
            throw new AccessDeniedException(resolveTermViewMessage(user, termResult));
        }
    }

    public void requireStudentTermResultPrintAccess(User user, TermResult termResult) {
        if (!canPrintTermResult(user, termResult)) {
            throw new AccessDeniedException(resolveTermPrintMessage(termResult));
        }
    }

    public void requireStudentSessionResultViewAccess(User user, SessionResult sessionResult) {
        if (!canViewSessionResult(user, sessionResult)) {
            throw new AccessDeniedException(resolveSessionViewMessage(user, sessionResult));
        }
    }

    public void requireStudentSessionResultPrintAccess(User user, SessionResult sessionResult) {
        if (!canPrintSessionResult(user, sessionResult)) {
            throw new AccessDeniedException(resolveSessionPrintMessage(sessionResult));
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
                    "You are not allowed to modify this student's result. Only admin, the form teacher, or the assigned subject teacher for this class can do this."
            );
        }
    }

    public void requireTermAssessmentModification(User user, Long studentId) {
        if (!canModifyStudentResult(user, studentId, null)) {
            throw new AccessDeniedException(
                    "You are not allowed to update this student's term assessment. Only admin or the form teacher can do this."
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

    public void requireClassTeacherOrAdmin(User user, Long classId) {
        SchoolClass schoolClass = findSchoolClass(classId);

        if (isAdmin(user)) {
            return;
        }

        if (!isTeacher(user)) {
            throw new AccessDeniedException("Only admin or assigned teacher can access this class");
        }

        if (user.getTeacher() == null) {
            throw new AccessDeniedException("Teacher account required");
        }

        if (!canAccessClass(user, schoolClass)) {
            throw new AccessDeniedException("You can only access your assigned class");
        }
    }

    public void requireResultClassAccess(User user, Long classId) {
        SchoolClass schoolClass = findSchoolClass(classId);

        if (isAdmin(user)) {
            return;
        }

        if (!isTeacher(user) || user.getTeacher() == null) {
            throw new AccessDeniedException("Only admin or assigned teacher can access these results");
        }

        boolean formTeacherMatch = isFormTeacherOfClass(user, schoolClass);

        log.info(
                "Result class access check => teacherId={}, classId={}, className='{}', arm='{}', formTeacherMatch={}",
                user.getTeacher().getId(),
                schoolClass.getId(),
                schoolClass.getClassName(),
                schoolClass.getArm(),
                formTeacherMatch
        );

        if (!formTeacherMatch) {
            throw new AccessDeniedException("You can only access results for your assigned class");
        }
    }

    public void requireResultClassAccess(User user, String className, String arm) {
        SchoolClass schoolClass = classRepository.findByClassNameAndArmNormalized(className, arm)
                .orElseThrow(() -> new AccessDeniedException("Class not found or inaccessible"));

        requireResultClassAccess(user, schoolClass.getId());
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

        boolean formTeacher = isFormTeacherOfStudent(user, student);

        log.info("Result access check => formTeacher={}", formTeacher);

        return formTeacher;
    }

    public boolean canViewTermResult(User user, TermResult termResult) {
        if (termResult == null || termResult.getStudent() == null) {
            return false;
        }

        Long studentId = termResult.getStudent().getId();

        if (isAdmin(user)) {
            return true;
        }

        if (isTeacher(user)) {
            return isFormTeacherOfStudent(user, studentId)
                    || canAccessClass(user, termResult.getStudent().getSchoolClass());
        }

        if (isOwnerStudent(user, studentId) || isParentOfStudent(user, studentId)) {
            return termResult.getVisibilityStatus() == ResultVisibilityStatus.PUBLISHED
                    || termResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE;
        }

        return false;
    }

    public boolean canPrintTermResult(User user, TermResult termResult) {
        if (termResult == null) {
            return false;
        }

        if (isAdmin(user) || isTeacher(user)) {
            return canViewTermResult(user, termResult);
        }

        if (!canViewTermResult(user, termResult)) {
            return false;
        }

        return termResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE
                && termResult.isPrintable();
    }

    public boolean canViewSessionResult(User user, SessionResult sessionResult) {
        if (sessionResult == null || sessionResult.getStudent() == null) {
            return false;
        }

        Long studentId = sessionResult.getStudent().getId();

        if (isAdmin(user)) {
            return true;
        }

        if (isTeacher(user)) {
            return isFormTeacherOfStudent(user, studentId)
                    || canAccessClass(user, sessionResult.getStudent().getSchoolClass());
        }

        if (isOwnerStudent(user, studentId) || isParentOfStudent(user, studentId)) {
            return sessionResult.getVisibilityStatus() == ResultVisibilityStatus.PUBLISHED
                    || sessionResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE;
        }

        return false;
    }

    public boolean canPrintSessionResult(User user, SessionResult sessionResult) {
        if (sessionResult == null) {
            return false;
        }

        if (isAdmin(user) || isTeacher(user)) {
            return canViewSessionResult(user, sessionResult);
        }

        if (!canViewSessionResult(user, sessionResult)) {
            return false;
        }

        return sessionResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE
                && sessionResult.isPrintable();
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

        log.info("Result modification decision => teacherId={}, studentId={}, classId={}, className={}, classArm={}, subjectId={}, formTeacher={}, subjectTeacher={}",
                user.getTeacher().getId(),
                studentId,
                getStudentClassId(student),
                getStudentClassName(student),
                getStudentClassArm(student),
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
                && Objects.equals(user.getStudent().getId(), studentId);
    }

    public boolean isParentOfStudent(User user, Long studentId) {
        if (!isParent(user) || user.getParent() == null) {
            return false;
        }

        Parent parent = user.getParent();

        return studentRepository.findById(studentId)
                .map(student -> student.getParent() != null && Objects.equals(student.getParent().getId(), parent.getId()))
                .orElse(false);
    }

    public boolean isFormTeacherOfStudent(User user, Long studentId) {
        Student student = findStudent(studentId);
        return student != null && isFormTeacherOfStudent(user, student);
    }

    private boolean isFormTeacherOfStudent(User user, Student student) {
        if (!isTeacher(user) || user.getTeacher() == null || student == null) {
            return false;
        }

        SchoolClass schoolClass = student.getSchoolClass();
        if (schoolClass == null || schoolClass.getId() == null) {
            return false;
        }

        SchoolClass resolvedClass = classRepository.findByIdWithTeacher(schoolClass.getId())
                .orElse(null);

        if (resolvedClass == null || resolvedClass.getClassTeacher() == null) {
            return false;
        }

        return Objects.equals(resolvedClass.getClassTeacher().getId(), user.getTeacher().getId());
    }

    private boolean isTeacherAssignedToStudentSubject(User user, Student student, Long subjectId) {
        if (!isTeacher(user) || user.getTeacher() == null || student == null || student.getSchoolClass() == null) {
            return false;
        }

        if (subjectId == null) {
            return false;
        }

        String studentClassName = getStudentClassName(student);
        String studentClassArm = getStudentClassArm(student);

        if (isBlank(studentClassName) || isBlank(studentClassArm)) {
            return false;
        }

        List<TeacherSubject> assignments =
                teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(user.getTeacher().getId());

        for (TeacherSubject assignment : assignments) {
            Long assignedSubjectId = assignment.getSubject() != null ? assignment.getSubject().getId() : null;

            boolean sameScope =
                    normalizeCompact(studentClassName).equals(normalizeCompact(assignment.getClassName()))
                            && normalizeCompact(studentClassArm).equals(normalizeCompact(assignment.getClassArm()));

            boolean subjectMatch = assignedSubjectId != null && Objects.equals(assignedSubjectId, subjectId);

            if (sameScope && subjectMatch) {
                return true;
            }
        }

        return false;
    }

    private boolean isFormTeacherOfClass(User user, SchoolClass schoolClass) {
        if (!isTeacher(user) || user.getTeacher() == null || schoolClass == null) {
            return false;
        }

        if (schoolClass.getClassTeacher() == null) {
            return false;
        }

        return Objects.equals(schoolClass.getClassTeacher().getId(), user.getTeacher().getId());
    }

    private boolean canAccessClass(User user, SchoolClass schoolClass) {
        if (schoolClass == null || user == null) {
            return false;
        }

        if (isAdmin(user)) {
            return true;
        }

        if (!isTeacher(user) || user.getTeacher() == null) {
            return false;
        }

        if (isFormTeacherOfClass(user, schoolClass)) {
            return true;
        }

        List<TeacherSubject> assignments =
                teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(user.getTeacher().getId());

        String className = schoolClass.getClassName();
        String arm = schoolClass.getArm();

        return assignments.stream().anyMatch(assignment ->
                normalizeCompact(className).equals(normalizeCompact(assignment.getClassName()))
                        && normalizeCompact(arm).equals(normalizeCompact(assignment.getClassArm()))
        );
    }

    private Student findStudent(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return studentRepository.findById(studentId).orElse(null);
    }

    private SchoolClass findSchoolClass(Long classId) {
        if (classId == null) {
            throw new AccessDeniedException("Class id is required");
        }

        return classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new AccessDeniedException("Class not found or inaccessible"));
    }

    private Long getStudentClassId(Student student) {
        return student != null && student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
    }

    private String getStudentClassName(Student student) {
        return student != null ? student.getStudentClass() : null;
    }

    private String getStudentClassArm(Student student) {
        return student != null ? student.getClassArm() : null;
    }

    private String normalizeCompact(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveTermViewMessage(User user, TermResult termResult) {
        if (isOwnerStudent(user, termResult.getStudent().getId()) || isParentOfStudent(user, termResult.getStudent().getId())) {
            return hasText(termResult.getVisibilityMessage())
                    ? termResult.getVisibilityMessage()
                    : "Result is not yet available for student or parent viewing.";
        }
        return "You are not allowed to view this student's result";
    }

    private String resolveTermPrintMessage(TermResult termResult) {
        return hasText(termResult.getPrintLockMessage())
                ? termResult.getPrintLockMessage()
                : "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    private String resolveSessionViewMessage(User user, SessionResult sessionResult) {
        if (isOwnerStudent(user, sessionResult.getStudent().getId()) || isParentOfStudent(user, sessionResult.getStudent().getId())) {
            return hasText(sessionResult.getVisibilityMessage())
                    ? sessionResult.getVisibilityMessage()
                    : "Session result is not yet available for student or parent viewing.";
        }
        return "You are not allowed to view this student's session result";
    }

    private String resolveSessionPrintMessage(SessionResult sessionResult) {
        return hasText(sessionResult.getPrintLockMessage())
                ? sessionResult.getPrintLockMessage()
                : "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}