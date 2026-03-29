package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.StudentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ClassRepository classRepository;

    @Autowired
    public StudentServiceImpl(
            StudentRepository studentRepository,
            ParentRepository parentRepository,
            ClassRepository classRepository
    ) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.classRepository = classRepository;
    }

    @Override
    public Student registerStudent(Student student) {
        log.info("Registering new student: {} {}", student.getFirstName(), student.getLastName());

        SchoolClass schoolClass = resolveRequiredClass(student);
        student.setSchoolClass(schoolClass);

        if (student.getAdmissionNumber() == null || student.getAdmissionNumber().isEmpty()) {
            student.setAdmissionNumber(generateAdmissionNumber());
        }

        if (student.getStatus() == null) {
            student.setStatus(Student.StudentStatus.ACTIVE);
        }

        if (student.getAdmissionDate() == null) {
            student.setAdmissionDate(LocalDate.now());
        }

        if (student.getProfilePictureUrl() == null || student.getProfilePictureUrl().isEmpty()) {
            student.setProfilePictureUrl("/uploads/profile-pictures/default.png");
        }

        linkParentIfPossible(student);

        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        log.info("Updating student with ID: {}", id);

        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        SchoolClass schoolClass = resolveRequiredClass(studentDetails);

        existingStudent.setFirstName(studentDetails.getFirstName());
        existingStudent.setLastName(studentDetails.getLastName());
        existingStudent.setMiddleName(studentDetails.getMiddleName());
        existingStudent.setSchoolClass(schoolClass);
        existingStudent.setGender(studentDetails.getGender());
        existingStudent.setDateOfBirth(studentDetails.getDateOfBirth());

        existingStudent.setParentName(studentDetails.getParentName());
        existingStudent.setParentPhone(studentDetails.getParentPhone());
        existingStudent.setParentEmail(studentDetails.getParentEmail());

        existingStudent.setAddress(studentDetails.getAddress());
        existingStudent.setLocalGovtArea(studentDetails.getLocalGovtArea());
        existingStudent.setStateOfOrigin(studentDetails.getStateOfOrigin());
        existingStudent.setNationality(studentDetails.getNationality());
        existingStudent.setReligion(studentDetails.getReligion());
        existingStudent.setStatus(studentDetails.getStatus());

        existingStudent.setEmergencyContactName(studentDetails.getEmergencyContactName());
        existingStudent.setEmergencyContactPhone(studentDetails.getEmergencyContactPhone());
        existingStudent.setEmergencyContactRelationship(studentDetails.getEmergencyContactRelationship());

        existingStudent.setExcludeFromPromotion(studentDetails.isExcludeFromPromotion());
        existingStudent.setPromotionHoldReason(studentDetails.getPromotionHoldReason());
        existingStudent.setPreviousSchool(studentDetails.getPreviousSchool());

        if (studentDetails.getProfilePictureUrl() != null && !studentDetails.getProfilePictureUrl().isEmpty()) {
            existingStudent.setProfilePictureUrl(studentDetails.getProfilePictureUrl());
        }

        linkParentIfPossible(existingStudent);

        return studentRepository.save(existingStudent);
    }

    private SchoolClass resolveRequiredClass(Student student) {
        if (student.getSchoolClass() == null || student.getSchoolClass().getId() == null) {
            throw new ResourceNotFoundException("Class is required");
        }

        return classRepository.findById(student.getSchoolClass().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private void linkParentIfPossible(Student student) {
        String parentEmail = normalize(student.getParentEmail());

        if (parentEmail == null) {
            student.setParent(null);
            return;
        }

        Optional<Parent> parentOpt = parentRepository.findByEmailIgnoreCase(parentEmail);

        if (parentOpt.isPresent()) {
            Parent parent = parentOpt.get();
            student.setParent(parent);

            if (isBlank(student.getParentName())) {
                student.setParentName(buildParentName(parent));
            }
            if (isBlank(student.getParentPhone())) {
                student.setParentPhone(parent.getPhoneNumber());
            }
        } else {
            log.warn("No parent record found for email: {}", parentEmail);
            student.setParent(null);
        }
    }

    private String buildParentName(Parent parent) {
        return Arrays.asList(parent.getFirstName(), parent.getMiddleName(), parent.getLastName())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getStudentClassName(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null;
    }

    private String getStudentClassArm(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getArm() : null;
    }

    private Long getStudentClassId(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
    }
    private SchoolClass resolveNextClass(Long currentClassId) {
        if (currentClassId == null) {
            return null;
        }

        SchoolClass currentClass = classRepository.findById(currentClassId)
                .orElse(null);

        if (currentClass == null) {
            return null;
        }

        String currentClassName = normalizeClassName(currentClass.getClassName());
        String currentArm = currentClass.getArm();

        String nextClassName = switch (currentClassName) {
            case "NURSERY" -> "PRIMARY 1";
            case "PRIMARY 1" -> "PRIMARY 2";
            case "PRIMARY 2" -> "PRIMARY 3";
            case "PRIMARY 3" -> "PRIMARY 4";
            case "PRIMARY 4" -> "PRIMARY 5";
            case "PRIMARY 5" -> "PRIMARY 6";
            case "PRIMARY 6" -> "JSS 1";
            case "JSS 1" -> "JSS 2";
            case "JSS 2" -> "JSS 3";
            case "JSS 3" -> "SSS 1";
            case "SSS 1" -> "SSS 2";
            case "SSS 2" -> "SSS 3";
            case "SSS 3" -> null;
            default -> null;
        };

        if (nextClassName == null) {
            return null;
        }

        return findMatchingClass(nextClassName, currentArm);
    }

    private SchoolClass findMatchingClass(String className, String arm) {
        List<SchoolClass> allClasses = classRepository.findAll();

        for (SchoolClass schoolClass : allClasses) {
            if (schoolClass == null) continue;

            String dbClassName = normalizeClassName(schoolClass.getClassName());
            String dbArm = normalizeArm(schoolClass.getArm());

            if (Objects.equals(dbClassName, normalizeClassName(className))
                    && Objects.equals(dbArm, normalizeArm(arm))) {
                return schoolClass;
            }
        }

        for (SchoolClass schoolClass : allClasses) {
            if (schoolClass == null) continue;

            String dbClassName = normalizeClassName(schoolClass.getClassName());

            if (Objects.equals(dbClassName, normalizeClassName(className))) {
                return schoolClass;
            }
        }

        return null;
    }

    private String normalizeClassName(String value) {
        if (value == null) return null;

        String normalized = value.trim().replaceAll("\\s+", " ").toUpperCase();

        if (normalized.matches("^SS\\s*1$")) return "SSS 1";
        if (normalized.matches("^SS\\s*2$")) return "SSS 2";
        if (normalized.matches("^SS\\s*3$")) return "SSS 3";
        if (normalized.matches("^SSS\\s*1$")) return "SSS 1";
        if (normalized.matches("^SSS\\s*2$")) return "SSS 2";
        if (normalized.matches("^SSS\\s*3$")) return "SSS 3";
        if (normalized.matches("^JSS\\s*1$")) return "JSS 1";
        if (normalized.matches("^JSS\\s*2$")) return "JSS 2";
        if (normalized.matches("^JSS\\s*3$")) return "JSS 3";
        if (normalized.matches("^PRIMARY\\s*1$")) return "PRIMARY 1";
        if (normalized.matches("^PRIMARY\\s*2$")) return "PRIMARY 2";
        if (normalized.matches("^PRIMARY\\s*3$")) return "PRIMARY 3";
        if (normalized.matches("^PRIMARY\\s*4$")) return "PRIMARY 4";
        if (normalized.matches("^PRIMARY\\s*5$")) return "PRIMARY 5";
        if (normalized.matches("^PRIMARY\\s*6$")) return "PRIMARY 6";

        return normalized;
    }

    private String normalizeArm(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> getStudentByAdmissionNumber(String admissionNumber) {
        return studentRepository.findByAdmissionNumber(admissionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Student> getAllStudentsPaginated(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        studentRepository.delete(student);
    }

    @Override
    public void deleteStudentByAdmissionNumber(String admissionNumber) {
        Student student = studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with admission number: " + admissionNumber));
        studentRepository.delete(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> searchStudents(String searchTerm) {
        return studentRepository.searchByName(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByClassId(Long classId) {
        return studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByState(String stateOfOrigin) {
        return studentRepository.findByStateOfOriginOrderByLastNameAscFirstNameAsc(stateOfOrigin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByLGA(String lga) {
        return studentRepository.findByLocalGovtAreaOrderByLastNameAscFirstNameAsc(lga);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getActiveStudents() {
        return studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(Student.StudentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByStatus(Student.StudentStatus status) {
        return studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStudentCountByClass() {
        List<Object[]> results = studentRepository.countStudentsByClassWithArm();

        return results.stream()
                .collect(Collectors.toMap(
                        arr -> {
                            String className = (String) arr[1];
                            String arm = (String) arr[2];
                            return arm != null && !arm.isBlank() ? className + " - " + arm : className;
                        },
                        arr -> (Long) arr[3],
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalStudentCount() {
        return studentRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getActiveStudentCount() {
        return studentRepository.countByStatus(Student.StudentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getRecentAdmissions(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return studentRepository.findRecentAdmissions(cutoffDate);
    }

    @Override
    public List<Student> registerBulkStudents(List<Student> students) {
        return students.stream()
                .map(this::registerStudent)
                .collect(Collectors.toList());
    }

    @Override
    public void updateBulkStudentClass(List<Long> studentIds, Long newClassId) {
        List<Student> students = studentRepository.findAllById(studentIds);

        SchoolClass targetClass = classRepository.findById(newClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + newClassId));

        students.forEach(student -> student.setSchoolClass(targetClass));
        studentRepository.saveAll(students);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmissionNumberUnique(String admissionNumber) {
        return !studentRepository.existsByAdmissionNumber(admissionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateAdmissionNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = studentRepository.count() + 1;
        String sequential = String.format("%04d", count);
        return "NIS/" + year + "/" + sequential;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentReport(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateClassReport(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPromotionPreview() {
        List<Student> allStudents = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        Map<String, Long> currentDistribution = getStudentCountByClass();

        Map<String, Long> projectedDistribution = new LinkedHashMap<>();
        List<Map<String, Object>> promotions = new ArrayList<>();
        int excluded = 0;

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                continue;
            }

            String currentClass = getStudentClassName(student);
            String currentArm = getStudentClassArm(student);
            Long currentClassId = getStudentClassId(student);

            if (currentClass == null || currentClassId == null) {
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                promotions.add(Map.of(
                        "studentId", student.getId(),
                        "studentName", student.getFirstName() + " " + student.getLastName(),
                        "currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass,
                        "nextClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass,
                        "excluded", true,
                        "reason", student.getPromotionHoldReason()
                ));
                projectedDistribution.merge(
                        currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass,
                        1L,
                        Long::sum
                );
                continue;
            }

            SchoolClass nextClass = resolveNextClass(currentClassId);

            String nextClassLabel = nextClass != null
                    ? (nextClass.getArm() != null && !nextClass.getArm().isBlank()
                    ? nextClass.getClassName() + " - " + nextClass.getArm()
                    : nextClass.getClassName())
                    : "GRADUATED";

            promotions.add(Map.of(
                    "studentId", student.getId(),
                    "studentName", student.getFirstName() + " " + student.getLastName(),
                    "currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass,
                    "nextClass", nextClassLabel,
                    "excluded", false
            ));

            projectedDistribution.merge(nextClassLabel, 1L, Long::sum);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("currentDistribution", currentDistribution);
        response.put("projectedDistribution", projectedDistribution);
        response.put("promotions", promotions);
        response.put("excludedCount", excluded);
        response.put("eligibleCount", promotions.size() - excluded);

        return response;
    }

    @Override
    public Map<String, Object> promoteAllStudents() {
        List<Student> allStudents = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));

        int promoted = 0;
        int graduated = 0;
        int excluded = 0;
        List<String> promotedStudents = new ArrayList<>();

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                continue;
            }

            Long currentClassId = getStudentClassId(student);
            if (currentClassId == null) {
                continue;
            }

            SchoolClass nextClass = resolveNextClass(currentClassId);
            if (nextClass == null) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                graduated++;
                promotedStudents.add(student.getFirstName() + " " + student.getLastName() + " => GRADUATED");
            } else {
                student.setSchoolClass(nextClass);
                promoted++;
                promotedStudents.add(student.getFirstName() + " " + student.getLastName() + " => " + nextClass.getClassName() + (nextClass.getArm() != null ? " - " + nextClass.getArm() : ""));
            }

            studentRepository.save(student);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("promotedCount", promoted);
        response.put("graduatedCount", graduated);
        response.put("excludedCount", excluded);
        response.put("details", promotedStudents);

        return response;
    }

    @Override
    public Map<String, Object> promoteSelectedStudents(List<Long> studentIds) {
        List<Student> students = studentRepository.findAllById(studentIds);

        int promoted = 0;
        int graduated = 0;
        int excluded = 0;
        List<String> details = new ArrayList<>();

        for (Student student : students) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                continue;
            }

            Long currentClassId = getStudentClassId(student);
            if (currentClassId == null) {
                continue;
            }

            SchoolClass nextClass = resolveNextClass(currentClassId);
            if (nextClass == null) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                graduated++;
                details.add(student.getFirstName() + " " + student.getLastName() + " => GRADUATED");
            } else {
                student.setSchoolClass(nextClass);
                promoted++;
                details.add(student.getFirstName() + " " + student.getLastName() + " => " + nextClass.getClassName() + (nextClass.getArm() != null ? " - " + nextClass.getArm() : ""));
            }

            studentRepository.save(student);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("promotedCount", promoted);
        response.put("graduatedCount", graduated);
        response.put("excludedCount", excluded);
        response.put("details", details);

        return response;
    }

    @Override
    public Map<String, Object> promoteClass(Long classId) {
        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);

        int promoted = 0;
        int graduated = 0;
        int excluded = 0;
        List<String> details = new ArrayList<>();

        for (Student student : students) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                continue;
            }

            SchoolClass nextClass = resolveNextClass(classId);
            if (nextClass == null) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                graduated++;
                details.add(student.getFirstName() + " " + student.getLastName() + " => GRADUATED");
            } else {
                student.setSchoolClass(nextClass);
                promoted++;
                details.add(student.getFirstName() + " " + student.getLastName() + " => " + nextClass.getClassName() + (nextClass.getArm() != null ? " - " + nextClass.getArm() : ""));
            }

            studentRepository.save(student);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("promotedCount", promoted);
        response.put("graduatedCount", graduated);
        response.put("excludedCount", excluded);
        response.put("details", details);

        return response;
    }

    @Override
    public Student togglePromotionExclusion(Long studentId, boolean exclude, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setExcludeFromPromotion(exclude);
        student.setPromotionHoldReason(exclude ? reason : null);

        return studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getExcludedStudents() {
        return studentRepository.findByExcludeFromPromotionTrueOrderByLastNameAscFirstNameAsc();
    }
}