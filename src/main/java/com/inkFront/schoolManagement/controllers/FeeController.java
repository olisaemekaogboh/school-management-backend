package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.FeeService;
import com.inkFront.schoolManagement.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;
    private final AccessControlService accessControlService;
    private final SecurityUtils securityUtils;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    @PostMapping
    public ResponseEntity<?> createFee(@Valid @RequestBody FeeDTO feeDTO) {
        try {
            accessControlService.requireAdmin(currentUser());
            return new ResponseEntity<>(FeeDTO.fromFee(feeService.createFee(feeDTO)), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createBulkFees(@Valid @RequestBody List<FeeDTO> feeDTOs) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.createBulkFees(feeDTOs)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(dtos, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFee(@PathVariable Long id, @Valid @RequestBody FeeDTO feeDTO) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.updateFee(id, feeDTO)));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFee(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.getFee(id)));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFee(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            feeService.deleteFee(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFees(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) Fee.Term term,
            @RequestParam(required = false) Fee.PaymentStatus status) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getAllFees(session, term, status)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/paginated")
    public ResponseEntity<?> getAllFeesPaginated(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) Fee.Term term,
            @RequestParam(required = false) Fee.PaymentStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            accessControlService.requireAdmin(currentUser());

            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<FeeDTO> feePage = feeService.getAllFeesPaginated(session, term, status, pageable)
                    .map(FeeDTO::fromFee);

            return ResponseEntity.ok(feePage);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentFees(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);

            List<FeeDTO> dtos = feeService.getStudentFees(studentId, session, term)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/all")
    public ResponseEntity<?> getStudentAllFees(@PathVariable Long studentId) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);

            List<FeeDTO> dtos = feeService.getStudentAllFees(studentId)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyFees(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            User user = currentUser();
            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            List<FeeDTO> dtos = feeService.getStudentFees(user.getStudent().getId(), session, term)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/{feeId}/payment")
    public ResponseEntity<?> recordPayment(
            @PathVariable Long feeId,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String notes) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(
                    FeeDTO.fromFee(feeService.recordPayment(feeId, amount, paymentMethod, reference, notes))
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{feeId}/partial-payment")
    public ResponseEntity<?> recordPartialPayment(
            @PathVariable Long feeId,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(
                    FeeDTO.fromFee(feeService.recordPartialPayment(feeId, amount, paymentMethod, reference))
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getFeeStatistics(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getFeeStatistics(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/defaulters")
    public ResponseEntity<?> getDefaultingStudents(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getDefaultingStudents(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueFees() {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getOverdueFees()
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingFees(@RequestParam(defaultValue = "7") int days) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getUpcomingFees(days)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/has-outstanding")
    public ResponseEntity<?> hasOutstandingFees(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);
            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "hasOutstanding", feeService.hasOutstandingFees(studentId, session, term)
            ));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/outstanding/total")
    public ResponseEntity<?> getTotalOutstanding(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);
            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "totalOutstanding", feeService.getTotalOutstanding(studentId, session, term)
            ));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/outstanding/by-type")
    public ResponseEntity<?> getOutstandingByType(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);
            return ResponseEntity.ok(feeService.getOutstandingByType(studentId, session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<Resource> generateFeeReportPdf(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        accessControlService.requireAdmin(currentUser());

        byte[] pdfBytes = feeService.generateFeeReportPdf(session, term);
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee-report.pdf")
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/report/excel")
    public ResponseEntity<Resource> generateFeeReportExcel(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        accessControlService.requireAdmin(currentUser());

        byte[] excelBytes = feeService.generateFeeReportExcel(session, term);
        ByteArrayResource resource = new ByteArrayResource(excelBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee-report.xlsx")
                .contentLength(excelBytes.length)
                .body(resource);
    }

    @GetMapping("/{feeId}/receipt")
    public ResponseEntity<Resource> generateReceipt(@PathVariable Long feeId) {
        accessControlService.requireAdmin(currentUser());

        byte[] pdfBytes = feeService.generateReceipt(feeId);
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + feeId + ".pdf")
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/due-between")
    public ResponseEntity<?> getFeesDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            accessControlService.requireAdmin(currentUser());

            List<FeeDTO> dtos = feeService.getFeesDueBetween(start, end)
                    .stream()
                    .map(FeeDTO::fromFee)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }
}