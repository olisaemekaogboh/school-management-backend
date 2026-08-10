package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.GeneratedResultCheckerPinDTO;
import com.inkFront.schoolManagement.dto.ResultCheckerPinCreateDTO;
import com.inkFront.schoolManagement.dto.ResultCheckerPinResponseDTO;
import com.inkFront.schoolManagement.dto.ResultPinVerificationResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultCheckerPin;
import com.inkFront.schoolManagement.model.ResultCheckerPinUsage;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ResultCheckerPinRepository;
import com.inkFront.schoolManagement.repository.ResultCheckerPinUsageRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.ResultCheckerPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultCheckerPinServiceImpl implements ResultCheckerPinService {

    private static final String PIN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PIN_LENGTH = 12;

    private final ResultCheckerPinRepository resultCheckerPinRepository;
    private final ResultCheckerPinUsageRepository resultCheckerPinUsageRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public List<GeneratedResultCheckerPinDTO> generatePins(
            ResultCheckerPinCreateDTO request,
            String createdByName
    ) {
        ResultCheckerPin.PinScope pinScope = parsePinScope(request.getPinScope());
        ResultCheckerPin.TargetType targetType = parseTargetType(request.getTargetType());

        if (pinScope == ResultCheckerPin.PinScope.TERM && request.getTerm() == null) {
            throw new IllegalArgumentException("term is required for TERM pins");
        }

        Student student = null;
        SchoolClass schoolClass = null;

        if (targetType == ResultCheckerPin.TargetType.STUDENT) {
            if (request.getStudentId() == null) {
                throw new IllegalArgumentException("studentId is required for STUDENT targetType");
            }
            student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student not found with id: " + request.getStudentId()
                    ));
        }

        if (targetType == ResultCheckerPin.TargetType.CLASS) {
            if (request.getClassId() == null) {
                throw new IllegalArgumentException("classId is required for CLASS targetType");
            }
            schoolClass = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Class not found with id: " + request.getClassId()
                    ));
        }

        int pinCount = resolveRequestedPinCount(request);

        List<GeneratedResultCheckerPinDTO> response = new ArrayList<>();

        for (int i = 0; i < pinCount; i++) {
            String rawPin = generateRawPin();

            ResultCheckerPin pin = new ResultCheckerPin();
            pin.setPinHash(passwordEncoder.encode(rawPin));
            pin.setPinScope(pinScope);
            pin.setTargetType(targetType);
            pin.setStudent(student);
            pin.setSchoolClass(schoolClass);
            pin.setSession(normalizeSession(request.getSession()));
            pin.setTerm(request.getTerm());
            pin.setMaxUsage(resolveMaxUsage(request));
            pin.setUsedCount(0);
            pin.setActive(true);
            pin.setExpiresAt(request.getExpiresAt());
            pin.setNotes(request.getNotes());
            pin.setCreatedByName(createdByName);

            ResultCheckerPin saved = resultCheckerPinRepository.save(pin);

            response.add(new GeneratedResultCheckerPinDTO(
                    saved.getId(),
                    rawPin,
                    saved.getPinScope().name(),
                    saved.getTargetType().name(),
                    saved.getStudent() != null ? saved.getStudent().getId() : null,
                    saved.getSchoolClass() != null ? saved.getSchoolClass().getId() : null,
                    saved.getSession(),
                    saved.getTerm(),
                    saved.getMaxUsage(),
                    saved.getUsedCount(),
                    saved.isActive(),
                    saved.getExpiresAt(),
                    saved.getNotes(),
                    saved.getCreatedByName(),
                    saved.getCreatedAt()
            ));
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultCheckerPinResponseDTO> getAllPins() {
        return resultCheckerPinRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ResultCheckerPinResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public ResultCheckerPinResponseDTO deactivatePin(Long pinId) {
        ResultCheckerPin pin = resultCheckerPinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("PIN not found with id: " + pinId));

        pin.setActive(false);
        return ResultCheckerPinResponseDTO.fromEntity(resultCheckerPinRepository.save(pin));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPinVerificationResponseDTO verifyTermPin(
            Long studentId,
            String session,
            String term,
            String rawPin
    ) {
        Student student = findStudent(studentId);
        Result.Term normalizedTerm = parseResultTerm(term);
        ResultCheckerPin matched = resolveMatchingTermPin(student, session, normalizedTerm, rawPin);

        return new ResultPinVerificationResponseDTO(
                true,
                "PIN is valid for this term result.",
                matched.getId(),
                Math.max(matched.getMaxUsage() - matched.getUsedCount(), 0)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPinVerificationResponseDTO verifySessionPin(
            Long studentId,
            String session,
            String rawPin
    ) {
        Student student = findStudent(studentId);
        ResultCheckerPin matched = resolveMatchingSessionPin(student, session, rawPin);

        return new ResultPinVerificationResponseDTO(
                true,
                "PIN is valid for this session result.",
                matched.getId(),
                Math.max(matched.getMaxUsage() - matched.getUsedCount(), 0)
        );
    }

    @Override
    public void consumeTermPin(
            Long studentId,
            String session,
            String term,
            String rawPin,
            String usedByName
    ) {
        Student student = findStudent(studentId);
        Result.Term normalizedTerm = parseResultTerm(term);
        ResultCheckerPin matched = resolveMatchingTermPin(student, session, normalizedTerm, rawPin);
        consumePin(matched, student, normalizeSession(session), normalizedTerm, usedByName);
    }

    @Override
    public void consumeSessionPin(
            Long studentId,
            String session,
            String rawPin,
            String usedByName
    ) {
        Student student = findStudent(studentId);
        ResultCheckerPin matched = resolveMatchingSessionPin(student, session, rawPin);
        consumePin(matched, student, normalizeSession(session), null, usedByName);
    }

    private void consumePin(
            ResultCheckerPin pin,
            Student student,
            String session,
            Result.Term term,
            String usedByName
    ) {
        if (!pin.canStillBeUsed()) {
            throw new IllegalArgumentException("This PIN has expired, been deactivated, or exhausted its allowed usage.");
        }

        pin.setUsedCount(pin.getUsedCount() + 1);
        resultCheckerPinRepository.save(pin);

        ResultCheckerPinUsage usage = new ResultCheckerPinUsage();
        usage.setPin(pin);
        usage.setStudent(student);
        usage.setPinScope(pin.getPinScope());
        usage.setSession(session);
        usage.setTerm(term);
        usage.setUsedByName(usedByName);

        resultCheckerPinUsageRepository.save(usage);
    }

    private ResultCheckerPin resolveMatchingTermPin(
            Student student,
            String session,
            Result.Term term,
            String rawPin
    ) {
        String normalizedSession = normalizeSession(session);
        String normalizedPin = normalizeRawPin(rawPin);

        List<ResultCheckerPin> candidates = resultCheckerPinRepository
                .findByPinScopeAndSessionAndTermOrderByCreatedAtDesc(
                        ResultCheckerPin.PinScope.TERM,
                        normalizedSession,
                        term
                );

        return candidates.stream()
                .filter(ResultCheckerPin::isActive)
                .filter(pin -> matchesTarget(pin, student))
                .filter(pin -> !pin.isExpired())
                .filter(pin -> pin.getUsedCount() < pin.getMaxUsage())
                .filter(pin -> passwordEncoder.matches(normalizedPin, pin.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or unusable term result checker PIN."));
    }

    private ResultCheckerPin resolveMatchingSessionPin(
            Student student,
            String session,
            String rawPin
    ) {
        String normalizedSession = normalizeSession(session);
        String normalizedPin = normalizeRawPin(rawPin);

        List<ResultCheckerPin> candidates = resultCheckerPinRepository
                .findByPinScopeAndSessionOrderByCreatedAtDesc(
                        ResultCheckerPin.PinScope.SESSION,
                        normalizedSession
                );

        return candidates.stream()
                .filter(ResultCheckerPin::isActive)
                .filter(pin -> matchesTarget(pin, student))
                .filter(pin -> !pin.isExpired())
                .filter(pin -> pin.getUsedCount() < pin.getMaxUsage())
                .filter(pin -> passwordEncoder.matches(normalizedPin, pin.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or unusable session result checker PIN."));
    }

    private boolean matchesTarget(ResultCheckerPin pin, Student student) {
        if (pin.getTargetType() == ResultCheckerPin.TargetType.STUDENT) {
            return pin.getStudent() != null
                    && pin.getStudent().getId() != null
                    && pin.getStudent().getId().equals(student.getId());
        }

        return pin.getSchoolClass() != null
                && student.getSchoolClass() != null
                && pin.getSchoolClass().getId() != null
                && student.getSchoolClass().getId() != null
                && pin.getSchoolClass().getId().equals(student.getSchoolClass().getId());
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
    }

    private ResultCheckerPin.PinScope parsePinScope(String value) {
        try {
            return ResultCheckerPin.PinScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pinScope. Allowed values: TERM, SESSION");
        }
    }

    private ResultCheckerPin.TargetType parseTargetType(String value) {
        try {
            return ResultCheckerPin.TargetType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid targetType. Allowed values: STUDENT, CLASS");
        }
    }

    private Result.Term parseResultTerm(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("term is required");
        }

        try {
            return Result.Term.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid term. Allowed values: FIRST, SECOND, THIRD");
        }
    }

    private String normalizeSession(String session) {
        if (session == null || session.isBlank()) {
            throw new IllegalArgumentException("session is required");
        }
        return session.trim();
    }

    private String normalizeRawPin(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            throw new IllegalArgumentException("A valid result checker PIN is required.");
        }
        return rawPin.trim();
    }

    private int resolveRequestedPinCount(ResultCheckerPinCreateDTO request) {
        Integer count = request.getCount();
        if (count == null || count < 1) {
            throw new IllegalArgumentException("count must be at least 1");
        }
        return count;
    }

    private int resolveMaxUsage(ResultCheckerPinCreateDTO request) {
        Integer maxUsage = request.getMaxUsage();
        if (maxUsage == null || maxUsage < 1) {
            return 1;
        }
        return maxUsage;
    }

    private String generateRawPin() {
        StringBuilder builder = new StringBuilder("RC-");

        for (int i = 0; i < PIN_LENGTH; i++) {
            builder.append(PIN_CHARS.charAt(secureRandom.nextInt(PIN_CHARS.length())));
            if (i == 3 || i == 7) {
                builder.append('-');
            }
        }

        return builder.toString();
    }
}