package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.ClassSubject;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ClassSubjectRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.SubjectRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Override
    public SchoolClass createClass(ClassDTO classDTO) {
        String className = safe(classDTO.getClassName());
        String arm = safe(classDTO.getArm());

        if (className.isBlank()) {
            throw new RuntimeException("Class name is required");
        }

        if (arm.isBlank()) {
            throw new RuntimeException("Class arm is required");
        }

        if (classRepository.existsByClassNameAndArm(className, arm)) {
            throw new RuntimeException("Class already exists: " + className + " " + arm);
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .className(className)
                .arm(arm)
                .category(classDTO.getCategory())
                .description(classDTO.getDescription())
                .capacity(classDTO.getCapacity())
                .currentEnrollment(0)
                .build();

        if (classDTO.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(classDTO.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found with id: " + classDTO.getClassTeacherId()
                    ));
            schoolClass.setClassTeacher(teacher);
        }

        SchoolClass savedClass = classRepository.save(schoolClass);

        if (classDTO.getSubjects() != null && !classDTO.getSubjects().isEmpty()) {
            for (String subjectName : classDTO.getSubjects()) {
                String cleanSubject = safe(subjectName);
                if (cleanSubject.isBlank()) {
                    continue;
                }

                Subject subject = subjectRepository.findByNameIgnoreCase(cleanSubject)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Subject not found with name: " + cleanSubject));

                boolean exists = classSubjectRepository.findBySchoolClassAndSubject(savedClass, subject).isPresent();
                if (!exists) {
                    ClassSubject classSubject = ClassSubject.builder()
                            .schoolClass(savedClass)
                            .subject(subject)
                            .build();
                    classSubjectRepository.save(classSubject);
                }
            }
        }

        return classRepository.findByIdWithTeacher(savedClass.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found after creation"));
    }

    @Override
    public SchoolClass updateClass(Long id, ClassDTO classDTO) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        String className = safe(classDTO.getClassName());
        String arm = safe(classDTO.getArm());

        if (className.isBlank()) {
            throw new RuntimeException("Class name is required");
        }

        if (arm.isBlank()) {
            throw new RuntimeException("Class arm is required");
        }

        classRepository.findByClassNameAndArm(className, arm)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException(
                                "Another class already exists with name " + className + " and arm " + arm
                        );
                    }
                });

        schoolClass.setClassName(className);
        schoolClass.setArm(arm);
        schoolClass.setCategory(classDTO.getCategory());
        schoolClass.setDescription(classDTO.getDescription());
        schoolClass.setCapacity(classDTO.getCapacity());

        if (classDTO.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(classDTO.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found with id: " + classDTO.getClassTeacherId()
                    ));
            schoolClass.setClassTeacher(teacher);
        } else {
            schoolClass.setClassTeacher(null);
        }

        SchoolClass savedClass = classRepository.save(schoolClass);

        if (classDTO.getSubjects() != null) {
            List<ClassSubject> existingAssignments =
                    classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(savedClass);

            if (!existingAssignments.isEmpty()) {
                classSubjectRepository.deleteAll(existingAssignments);
            }

            for (String subjectName : classDTO.getSubjects()) {
                String cleanSubject = safe(subjectName);
                if (cleanSubject.isBlank()) {
                    continue;
                }

                Subject subject = subjectRepository.findByNameIgnoreCase(cleanSubject)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Subject not found with name: " + cleanSubject));

                ClassSubject classSubject = ClassSubject.builder()
                        .schoolClass(savedClass)
                        .subject(subject)
                        .build();

                classSubjectRepository.save(classSubject);
            }
        }

        return classRepository.findByIdWithTeacher(savedClass.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found after update"));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClass getClass(Long id) {
        return classRepository.findByIdWithTeacher(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClass getClassWithTeacher(Long id) {
        return classRepository.findByIdWithTeacher(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClass getClassByName(String className) {
        return classRepository.findByClassName(className)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with name: " + className));
    }

    @Override
    public void deleteClass(Long id) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        List<ClassSubject> assignments = classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(schoolClass);
        if (!assignments.isEmpty()) {
            classSubjectRepository.deleteAll(assignments);
        }

        classRepository.delete(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDTO> getAllClasses() {
        return classRepository.findAllWithTeacher()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDTO> getClassesByCategory(String category) {
        SchoolClass.ClassCategory parsedCategory;
        try {
            parsedCategory = SchoolClass.ClassCategory.valueOf(category.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid class category: " + category);
        }

        return classRepository.findByCategoryWithTeacher(parsedCategory)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public SchoolClass assignClassTeacher(Long classId, Long teacherId) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        schoolClass.setClassTeacher(teacher);
        SchoolClass saved = classRepository.save(schoolClass);

        return classRepository.findByIdWithTeacher(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found after assigning teacher"));
    }

    @Override
    public SchoolClass addSubject(Long classId, String subject) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        String cleanSubject = safe(subject);
        if (cleanSubject.isBlank()) {
            throw new RuntimeException("Subject is required");
        }

        Subject foundSubject = subjectRepository.findByNameIgnoreCase(cleanSubject)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with name: " + cleanSubject));

        boolean exists = classSubjectRepository.findBySchoolClassAndSubject(schoolClass, foundSubject).isPresent();
        if (!exists) {
            ClassSubject classSubject = ClassSubject.builder()
                    .schoolClass(schoolClass)
                    .subject(foundSubject)
                    .build();

            classSubjectRepository.save(classSubject);
        }

        return classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found after adding subject"));
    }

    @Override
    public SchoolClass removeSubject(Long classId, String subject) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        String cleanSubject = safe(subject);
        if (cleanSubject.isBlank()) {
            return schoolClass;
        }

        Subject foundSubject = subjectRepository.findByNameIgnoreCase(cleanSubject)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with name: " + cleanSubject));

        classSubjectRepository.deleteBySchoolClassAndSubject(schoolClass, foundSubject);

        return classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found after removing subject"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getStudentsInClass(Long classId) {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        List<Student> students = studentRepository.findByStudentClassAndClassArm(
                schoolClass.getClassName(),
                schoolClass.getArm()
        );

        return students.stream()
                .map(StudentResponseDTO::fromStudent)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getClassStatistics() {
        List<SchoolClass> allClasses = classRepository.findAllWithTeacher();

        Map<String, Object> stats = new HashMap<>();

        int totalClasses = allClasses.size();
        int totalCapacity = 0;
        int totalEnrollment = 0;
        long classesWithTeacher = 0;

        Map<String, Long> byCategory = new HashMap<>();

        for (SchoolClass schoolClass : allClasses) {
            int studentCount = studentRepository.findByStudentClassAndClassArm(
                    schoolClass.getClassName(),
                    schoolClass.getArm()
            ).size();

            totalCapacity += schoolClass.getCapacity() != null ? schoolClass.getCapacity() : 0;
            totalEnrollment += studentCount;

            if (schoolClass.getClassTeacher() != null) {
                classesWithTeacher++;
            }

            String key = schoolClass.getCategory() != null
                    ? schoolClass.getCategory().name()
                    : "UNCATEGORIZED";
            byCategory.put(key, byCategory.getOrDefault(key, 0L) + 1);
        }

        int availableSeats = Math.max(totalCapacity - totalEnrollment, 0);

        stats.put("totalClasses", totalClasses);
        stats.put("totalStudents", totalEnrollment);
        stats.put("totalEnrollment", totalEnrollment);
        stats.put("totalCapacity", totalCapacity);
        stats.put("classesWithTeacher", classesWithTeacher);
        stats.put("availableSeats", availableSeats);
        stats.put("availableSpaces", availableSeats);
        stats.put("classesByCategory", byCategory);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateClassListPdf(Long classId) throws Exception {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        List<Student> students = studentRepository.findByStudentClassAndClassArm(
                schoolClass.getClassName(),
                schoolClass.getArm()
        );

        StringBuilder content = new StringBuilder();
        content.append("CLASS LIST\n");
        content.append("==========\n\n");
        content.append("Class: ")
                .append(schoolClass.getClassName())
                .append(" ")
                .append(schoolClass.getArm())
                .append("\n");
        content.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        int sn = 1;
        for (Student student : students) {
            content.append(sn++)
                    .append(". ")
                    .append(buildStudentFullName(student))
                    .append(" - ")
                    .append(value(student.getAdmissionNumber()))
                    .append("\n");
        }

        return content.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateClassListExcel(Long classId) throws Exception {
        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        List<Student> students = studentRepository.findByStudentClassAndClassArm(
                schoolClass.getClassName(),
                schoolClass.getArm()
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Class List");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("S/N");
            header.createCell(1).setCellValue("Admission Number");
            header.createCell(2).setCellValue("First Name");
            header.createCell(3).setCellValue("Last Name");
            header.createCell(4).setCellValue("Full Name");
            header.createCell(5).setCellValue("Gender");
            header.createCell(6).setCellValue("Class");
            header.createCell(7).setCellValue("Arm");

            int rowNum = 1;
            int sn = 1;

            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sn++);
                row.createCell(1).setCellValue(value(student.getAdmissionNumber()));
                row.createCell(2).setCellValue(value(student.getFirstName()));
                row.createCell(3).setCellValue(value(student.getLastName()));
                row.createCell(4).setCellValue(buildStudentFullName(student));
                row.createCell(5).setCellValue(student.getGender() != null ? student.getGender().name() : "");
                row.createCell(6).setCellValue(value(student.getStudentClass()));
                row.createCell(7).setCellValue(value(student.getClassArm()));
            }

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ClassDTO mapToDTO(SchoolClass schoolClass) {
        List<StudentResponseDTO> students = studentRepository
                .findByStudentClassAndClassArm(schoolClass.getClassName(), schoolClass.getArm())
                .stream()
                .map(StudentResponseDTO::fromStudent)
                .toList();

        List<String> subjects = classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(schoolClass)
                .stream()
                .map(cs -> cs.getSubject().getName())
                .toList();

        return ClassDTO.fromEntity(schoolClass, subjects, students);
    }

    private String buildStudentFullName(Student student) {
        return (
                value(student.getFirstName()) + " " +
                        value(student.getMiddleName()) + " " +
                        value(student.getLastName())
        ).trim().replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}