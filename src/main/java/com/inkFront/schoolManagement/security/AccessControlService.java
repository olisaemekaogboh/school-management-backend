package com.inkFront.schoolManagement.security;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

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

    public void requireStudentResultModification(User user, Long studentId) {
        if (!canModifyStudentResult(user, studentId)) {
            throw new AccessDeniedException("You are not allowed to modify this student's result");
        }
    }

    public void requireClassTeacherOrAdmin(User user, String className, String arm) {
        if (isAdmin(user)) {
            return;
        }

        if (!isTeacher(user)) {
            throw new AccessDeniedException("Only admin or form teacher can access this class");
        }

        if (user.getTeacher() == null) {
            throw new AccessDeniedException("Teacher account required");
        }

        SchoolClass schoolClass = classRepository.findByClassNameAndArm(className, arm)
                .orElseThrow(() -> new RuntimeException("Class not found: " + className + " " + arm));

        if (schoolClass.getClassTeacher() == null) {
            throw new AccessDeniedException("This class has no assigned form teacher");
        }

        if (!schoolClass.getClassTeacher().getId().equals(user.getTeacher().getId())) {
            throw new AccessDeniedException("Only the assigned form teacher can access this class");
        }
    }

    public boolean canAccessStudent(User user, Long studentId) {
        return isAdmin(user)
                || isOwnerStudent(user, studentId)
                || isParentOfStudent(user, studentId)
                || isFormTeacherOfStudent(user, studentId);
    }

    public boolean canModifyStudentResult(User user, Long studentId) {
        return isAdmin(user) || isFormTeacherOfStudent(user, studentId);
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
                .map(student -> student.getParent() != null
                        && student.getParent().getId().equals(parent.getId()))
                .orElse(false);
    }

    public boolean isFormTeacherOfStudent(User user, Long studentId) {
        if (!isTeacher(user) || user.getTeacher() == null) {
            return false;
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }

        if (student.getStudentClass() == null || student.getClassArm() == null) {
            return false;
        }

        SchoolClass schoolClass = classRepository
                .findByClassNameAndArm(student.getStudentClass(), student.getClassArm())
                .orElse(null);

        if (schoolClass == null || schoolClass.getClassTeacher() == null) {
            return false;
        }

        return schoolClass.getClassTeacher().getId().equals(user.getTeacher().getId());
    }
}