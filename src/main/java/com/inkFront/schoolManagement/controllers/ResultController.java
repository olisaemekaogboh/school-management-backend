package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.PrintableStatusUpdateDTO;
import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.dto.ResultResponseDTO;
import com.inkFront.schoolManagement.dto.TermAssessmentUpdateDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.ResultCheckerPinService;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.SessionResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);

    private final ResultService resultService;
    private final SessionResultService sessionResultService;
    private final TermResultRepository termResultRepository;
    private final ClassRepository classRepository;
    private final AccessControlService accessControlService;
    private final SecurityUtils securityUtils;
    private final ResultCheckerPinService resultCheckerPinService;
    private final StudentRepository studentRepository;
    private final SessionResultRepository sessionResultRepository;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private boolean isStudentOrParent(User user) {
        return user != null
                && user.getRole() != null
                && (user.getRole() == User.Role.STUDENT || user.getRole() == User.Role.PARENT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String currentUserDisplayName(User user) {
        if (user == null) {
            return "Unknown User";
        }

        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : ""))
                .replaceAll("\\s+", " ")
                .trim();

        return fullName.isBlank() ? user.getUsername() : fullName;
    }

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        log.error(message, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", message,
                        "error", e.getMessage()
                ));
    }

    private SchoolClass resolveClass(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
    }

    private TermResult findTermResult(Long studentId, String session, Result.Term term) {
        return termResultRepository.findByStudentIdAndSessionAndTerm(studentId, session, term)
                .orElseThrow(() -> new RuntimeException(
                        "Term result not found for student ID " + studentId + " in session " + session + " and term " + term
                ));
    }

    private SessionResult findSessionResult(Long studentId, String session) {
        return sessionResultRepository.findDetailedByStudentAndSession(
                        studentRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId)),
                        session
                )
                .orElseThrow(() -> new RuntimeException(
                        "Session result not found for student ID " + studentId + " in session " + session
                ));
    }

    private void consumeTermPin(Long studentId, String session, Result.Term term, String pin, User user) {
        resultCheckerPinService.consumeTermPin(
                studentId,
                session,
                String.valueOf(term),
                pin,
                currentUserDisplayName(user)
        );
    }

    private void consumeSessionPin(Long studentId, String session, String pin, User user) {
        resultCheckerPinService.consumeSessionPin(
                studentId,
                session,
                pin,
                currentUserDisplayName(user)
        );
    }

    /**
     * Access rule:
     * - Admin/teacher/staff continue using normal visibility/access rules.
     * - Student/parent may access either:
     *   1. through normal released visibility access, OR
     *   2. through a valid result checker PIN.
     */
    private void enforceTermResultAccess(
            User user,
            Long studentId,
            String session,
            Result.Term term,
            String pin,
            TermResult termResult
    ) {
        if (!isStudentOrParent(user)) {
            accessControlService.requireStudentTermResultViewAccess(user, termResult);
            return;
        }

        if (hasText(pin)) {
            consumeTermPin(studentId, session, term, pin.trim(), user);
            return;
        }

        accessControlService.requireStudentTermResultViewAccess(user, termResult);
    }

    /**
     * Access rule:
     * - Admin/teacher/staff continue using normal visibility/access rules.
     * - Student/parent may access either:
     *   1. through normal released visibility access, OR
     *   2. through a valid result checker PIN.
     */
    private void enforceSessionResultAccess(
            User user,
            Long studentId,
            String session,
            String pin,
            SessionResult sessionResult
    ) {
        if (!isStudentOrParent(user)) {
            accessControlService.requireStudentSessionResultViewAccess(user, sessionResult);
            return;
        }

        if (hasText(pin)) {
            consumeSessionPin(studentId, session, pin.trim(), user);
            return;
        }

        accessControlService.requireStudentSessionResultViewAccess(user, sessionResult);
    }

    @PostMapping("/student/{studentId}")
    public ResponseEntity<?> addOrUpdateResult(
            @PathVariable Long studentId,
            @Valid @RequestBody ResultRequestDTO resultRequest) {
        try {
            User user = currentUser();

            log.info("POST /api/results/student/{} => userId={}, role={}, requestStudentId={}, subjectId={}, session={}, term={}",
                    studentId,
                    user != null ? user.getId() : null,
                    user != null ? user.getRole() : null,
                    resultRequest.getStudentId(),
                    resultRequest.getSubjectId(),
                    resultRequest.getSession(),
                    resultRequest.getTerm());

            accessControlService.requireStudentResultModification(
                    user,
                    studentId,
                    resultRequest.getSubjectId()
            );

            resultRequest.setStudentId(studentId);

            Result result = resultService.addOrUpdateResult(resultRequest);

            log.info("Result saved successfully => studentId={}, subjectId={}, resultId={}",
                    studentId, resultRequest.getSubjectId(), result.getId());

            return ResponseEntity.ok(ResultResponseDTO.fromResult(result));
        } catch (AccessDeniedException e) {
            log.warn("Result save denied => studentId={}, subjectId={}, reason={}",
                    studentId, resultRequest.getSubjectId(), e.getMessage());
            return forbidden(e.getMessage());
        } catch (Exception e) {
            log.error("Result save failed => studentId={}, subjectId={}", studentId, resultRequest.getSubjectId(), e);
            return serverError("Unable to add or update result", e);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentResults(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireStudentResultAccess(user, studentId);

            List<Result> results = resultService.getStudentResults(studentId, session, term);
            List<ResultResponseDTO> response = results.stream()
                    .map(ResultResponseDTO::fromResult)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch student results", e);
        }
    }

    @GetMapping("/student/{studentId}/term")
    public ResponseEntity<?> getTermResult(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam(required = false) String pin) {
        try {
            User user = currentUser();
            TermResult termResult = findTermResult(studentId, session, term);

            enforceTermResultAccess(user, studentId, session, term, pin, termResult);

            Map<String, Object> resultSheet = resultService.generateResultSheet(studentId, session, term);
            return ResponseEntity.ok(resultSheet);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch term result", e);
        }
    }

    @PatchMapping("/student/{studentId}/term/printable")
    public ResponseEntity<?> setTermPrintable(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @Valid @RequestBody PrintableStatusUpdateDTO dto) {
        try {
            accessControlService.requireAdmin(currentUser());

            resultService.setTermResultPrintableStatus(
                    studentId,
                    session,
                    term,
                    Boolean.TRUE.equals(dto.getPrintable()),
                    dto.getPrintLockMessage()
            );

            return ResponseEntity.ok(
                    resultService.generateResultSheet(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update printable term result status", e);
        }
    }

    @GetMapping("/student/{studentId}/annual")
    public ResponseEntity<?> getAnnualResult(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam(required = false) String pin) {
        try {
            User user = currentUser();
            SessionResult sessionResult = findSessionResult(studentId, session);

            enforceSessionResultAccess(user, studentId, session, pin, sessionResult);

            return ResponseEntity.ok(sessionResultService.getSessionResult(studentId, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch annual result", e);
        }
    }

    @GetMapping("/me/term")
    public ResponseEntity<?> getMyTermResult(
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam(required = false) String pin) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            Long studentId = user.getStudent().getId();
            TermResult termResult = findTermResult(studentId, session, term);

            enforceTermResultAccess(user, studentId, session, term, pin, termResult);

            return ResponseEntity.ok(resultService.generateResultSheet(studentId, session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch your term result", e);
        }
    }

    @GetMapping("/me/annual")
    public ResponseEntity<?> getMyAnnualResult(
            @RequestParam String session,
            @RequestParam(required = false) String pin) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            Long studentId = user.getStudent().getId();
            SessionResult sessionResult = findSessionResult(studentId, session);

            enforceSessionResultAccess(user, studentId, session, pin, sessionResult);

            return ResponseEntity.ok(sessionResultService.getSessionResult(studentId, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch your annual result", e);
        }
    }

    @GetMapping("/rankings/class/{classId}")
    public ResponseEntity<?> getClassRankings(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            SchoolClass schoolClass = resolveClass(classId);

            accessControlService.requireClassTeacherOrAdmin(user, classId);

            Map<String, Object> rankings =
                    resultService.getClassRankings(
                            classId,
                            session,
                            term
                    );

            if (rankings instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mutable = (Map<String, Object>) map;
                mutable.put("classId", schoolClass.getId());
                mutable.put("className", schoolClass.getClassName());
                mutable.put("arm", schoolClass.getArm());
                mutable.put("classCode", schoolClass.getClassCode());
                return ResponseEntity.ok(mutable);
            }

            return ResponseEntity.ok(rankings);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class rankings", e);
        }
    }

    @GetMapping("/rankings/school")
    public ResponseEntity<?> getSchoolRankings(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            Map<String, Object> rankings = resultService.getSchoolRankings(session, term);
            return ResponseEntity.ok(rankings);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch school rankings", e);
        }
    }

    @PostMapping("/calculate/term")
    public ResponseEntity<?> calculateAllTermResults(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            resultService.calculateAllTermResults(session, term);
            return ResponseEntity.ok(Map.of("message", "All term results calculated successfully"));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate all term results", e);
        }
    }

    @PutMapping("/student/{studentId}/term/assessment")
    public ResponseEntity<?> updateTermAssessment(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @Valid @RequestBody TermAssessmentUpdateDTO dto) {
        try {
            User user = currentUser();
            accessControlService.requireTermAssessmentModification(user, studentId);

            resultService.updateTermAssessment(studentId, session, term, dto);

            return ResponseEntity.ok(
                    resultService.generateResultSheet(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update term assessment", e);
        }
    }

    @PatchMapping("/student/{studentId}/term/sign/class-teacher")
    public ResponseEntity<?> signByClassTeacher(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            accessControlService.requireTermAssessmentModification(user, studentId);

            String signatureUrl = user.getSignatureUrl();

            resultService.signByClassTeacher(studentId, session, term, signatureUrl);

            return ResponseEntity.ok(
                    resultService.generateResultSheet(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to sign result as class teacher", e);
        }
    }

    @PatchMapping("/student/{studentId}/term/sign/admin")
    public ResponseEntity<?> signByAdmin(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            accessControlService.requireAdmin(user);

            String signatureUrl = user.getSignatureUrl();

            resultService.signByAdmin(studentId, session, term, signatureUrl);

            return ResponseEntity.ok(
                    resultService.generateResultSheet(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to approve result", e);
        }
    }

    @PostMapping("/calculate/annual")
    public ResponseEntity<?> calculateAllSessionResults(@RequestParam String session) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            return ResponseEntity.ok(sessionResultService.calculateAllSessionResults(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate annual results", e);
        }
    }

    @GetMapping("/statistics/class/{classId}")
    public ResponseEntity<?> getClassStatistics(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            SchoolClass schoolClass = resolveClass(classId);

            accessControlService.requireClassTeacherOrAdmin(user, classId);

            List<TermResult> classResults = termResultRepository
                    .findByStudent_SchoolClass_ClassNameAndStudent_SchoolClass_ArmAndSessionAndTermOrderByAverageDesc(
                            schoolClass.getClassName(), schoolClass.getArm(), session, term
                    );

            if (classResults.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No results found for this class",
                        "classId", classId,
                        "className", schoolClass.getClassName(),
                        "arm", schoolClass.getArm(),
                        "classCode", schoolClass.getClassCode()
                ));
            }

            double classAverage = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .average()
                    .orElse(0);

            double highestScore = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .max()
                    .orElse(0);

            double lowestScore = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .min()
                    .orElse(0);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", classResults.size());
            stats.put("classAverage", classAverage);
            stats.put("highestScore", highestScore);
            stats.put("lowestScore", lowestScore);
            stats.put("classId", schoolClass.getId());
            stats.put("className", schoolClass.getClassName());
            stats.put("arm", schoolClass.getArm());
            stats.put("classCode", schoolClass.getClassCode());
            stats.put("session", session);
            stats.put("term", term);

            return ResponseEntity.ok(stats);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class statistics", e);
        }
    }
}