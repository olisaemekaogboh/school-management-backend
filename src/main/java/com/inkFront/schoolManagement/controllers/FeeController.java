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
@CrossOrigin(origins = "https://localhost:3000")
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

    // ==================== ADMIN CRUD ====================

    @PostMapping
    public ResponseEntity<?> createFee(@Valid @RequestBody FeeDTO feeDTO) {
        try {
            accessControlService.requireAdmin(currentUser());
            return new ResponseEntity<>(FeeDTO.fromFee(feeService.createFee(feeDTO)), HttpStatus.CREATED);
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
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFee(@PathVariable Long id, @Valid @RequestBody FeeDTO feeDTO) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.updateFee(id, feeDTO)));
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
            Page<FeeDTO> dtos = feeService.getAllFeesPaginated(session, term, status, pageable)
                    .map(FeeDTO::fromFee);

            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    // ==================== STUDENT / PARENT SAFE ACCESS ====================

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

    @GetMapping("/student/{studentId}/payments")
    public ResponseEntity<?> getStudentPaymentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) String session) {
        try {
            accessControlService.requireFeeAccess(currentUser(), studentId);
            return ResponseEntity.ok(feeService.getPaymentHistory(studentId, session));
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
            return ResponseEntity.ok(feeService.hasOutstandingFees(studentId, session, term));
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
            return ResponseEntity.ok(feeService.getTotalOutstanding(studentId, session, term));
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to fetch your fees", "error", e.getMessage()));
        }
    }

    @GetMapping("/me/payments")
    public ResponseEntity<?> getMyPaymentHistory(@RequestParam(required = false) String session) {
        try {
            User user = currentUser();
            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            return ResponseEntity.ok(feeService.getPaymentHistory(user.getStudent().getId(), session));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to fetch your payment history", "error", e.getMessage()));
        }
    }

    // ==================== ADMIN PAYMENT OPERATIONS ====================

    @PostMapping("/{id}/payment")
    public ResponseEntity<?> recordPayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String notes) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(
                    feeService.recordPayment(id, amount, paymentMethod, reference, notes)
            ));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/partial-payment")
    public ResponseEntity<?> recordPartialPayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(
                    feeService.recordPartialPayment(id, amount, paymentMethod, reference)
            ));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/bulk-payment")
    public ResponseEntity<?> recordBulkPayment(
            @RequestParam List<Long> feeIds,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.recordBulkPayment(feeIds, amount, paymentMethod, reference));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<?> reversePayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.reversePayment(id, reason)));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/waive")
    public ResponseEntity<?> waiveFee(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.waiveFee(id, reason)));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/mark-overdue")
    public ResponseEntity<?> markAsOverdue(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.markAsOverdue(id)));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<?> reopenFee(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(FeeDTO.fromFee(feeService.reopenFee(id)));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    // ==================== ADMIN STATISTICS / REPORTS ====================

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

    @GetMapping("/statistics/detailed")
    public ResponseEntity<?> getDetailedStatistics(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getDetailedStatistics(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/statistics/class-wise")
    public ResponseEntity<?> getClassWiseSummary(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getClassWiseSummary(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/statistics/monthly")
    public ResponseEntity<?> getMonthlyCollectionReport() {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getMonthlyCollectionReport());
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

    @GetMapping("/defaulters/top")
    public ResponseEntity<?> getTopDefaulters(
            @RequestParam String session,
            @RequestParam Fee.Term term,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getTopDefaulters(session, term, limit));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/defaulters/count")
    public ResponseEntity<?> countDefaulters(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.countDefaulters(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/reminders/send")
    public ResponseEntity<?> sendFeeReminders(
            @RequestParam String session,
            @RequestParam Fee.Term term,
            @RequestParam(defaultValue = "7") int daysBeforeDue) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.sendFeeReminders(session, term, daysBeforeDue));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/reminders/overdue")
    public ResponseEntity<?> sendOverdueReminders() {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.sendOverdueReminders());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/{id}/send-reminder")
    public ResponseEntity<?> sendSingleReminder(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.sendSingleReminder(id));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/reminders/bulk")
    public ResponseEntity<?> sendBulkReminders(@RequestParam List<Long> feeIds) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.sendBulkReminders(feeIds));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueFees() {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getOverdueFees().stream().map(FeeDTO::fromFee).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/overdue/filtered")
    public ResponseEntity<?> getOverdueFees(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getOverdueFees(session, term).stream().map(FeeDTO::fromFee).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingFees(@RequestParam(defaultValue = "7") int days) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getUpcomingFees(days).stream().map(FeeDTO::fromFee).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/due-between")
    public ResponseEntity<?> getFeesDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<FeeDTO> dtos = feeService.getFeesDueBetween(start, end).stream().map(FeeDTO::fromFee).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/due-today/count")
    public ResponseEntity<?> countFeesDueToday() {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.countFeesDueToday());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<?> getFeePaymentHistory(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getFeePaymentHistory(id));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/payments/recent")
    public ResponseEntity<?> getRecentPayments(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getRecentPayments(limit));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/report")
    public ResponseEntity<?> generateFeeReport(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.generateFeeReport(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<?> generateFeeReportPdf(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            byte[] reportBytes = feeService.generateFeeReportPdf(session, term);
            ByteArrayResource resource = new ByteArrayResource(reportBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee_report_" + session + "_" + term + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(reportBytes.length)
                    .body(resource);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/report/excel")
    public ResponseEntity<?> generateFeeReportExcel(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            byte[] reportBytes = feeService.generateFeeReportExcel(session, term);
            ByteArrayResource resource = new ByteArrayResource(reportBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee_report_" + session + "_" + term + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(reportBytes.length)
                    .body(resource);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> generateReceipt(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            byte[] receiptBytes = feeService.generateReceipt(id);
            ByteArrayResource resource = new ByteArrayResource(receiptBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(receiptBytes.length)
                    .body(resource);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PutMapping("/update-due-dates")
    public ResponseEntity<?> updateFeesDueDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate oldDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.updateFeesDueDate(oldDate, newDate));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getDashboardData(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/payment-trends")
    public ResponseEntity<?> getPaymentTrends(
            @RequestParam(defaultValue = "12") int months) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(feeService.getPaymentTrends(months));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }
}