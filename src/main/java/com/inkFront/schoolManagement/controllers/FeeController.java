// src/main/java/com/inkFront/schoolManagement/controllers/FeeController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.model.Fee;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    // ==================== CRUD Operations ====================

    @PostMapping
    public ResponseEntity<FeeDTO> createFee(@Valid @RequestBody FeeDTO feeDTO) {
        Fee fee = feeService.createFee(feeDTO);
        return new ResponseEntity<>(FeeDTO.fromFee(fee), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<FeeDTO>> createBulkFees(@Valid @RequestBody List<FeeDTO> feeDTOs) {
        List<Fee> fees = feeService.createBulkFees(feeDTOs);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeDTO> updateFee(@PathVariable Long id, @Valid @RequestBody FeeDTO feeDTO) {
        Fee fee = feeService.updateFee(id, feeDTO);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeDTO> getFee(@PathVariable Long id) {
        Fee fee = feeService.getFee(id);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFee(@PathVariable Long id) {
        feeService.deleteFee(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Fee Retrieval ====================

    @GetMapping
    public ResponseEntity<List<FeeDTO>> getAllFees(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) Fee.Term term,
            @RequestParam(required = false) Fee.PaymentStatus status) {
        List<Fee> fees = feeService.getAllFees(session, term, status);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<FeeDTO>> getAllFeesPaginated(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) Fee.Term term,
            @RequestParam(required = false) Fee.PaymentStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Fee> fees = feeService.getAllFeesPaginated(session, term, status, pageable);
        Page<FeeDTO> dtos = fees.map(FeeDTO::fromFee);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<FeeDTO>> getStudentFees(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        List<Fee> fees = feeService.getStudentFees(studentId, session, term);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/student/{studentId}/all")
    public ResponseEntity<List<FeeDTO>> getStudentAllFees(@PathVariable Long studentId) {
        List<Fee> fees = feeService.getStudentAllFees(studentId);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ==================== Payment Operations ====================

    @PostMapping("/{id}/payment")
    public ResponseEntity<FeeDTO> recordPayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String notes) {
        Fee fee = feeService.recordPayment(id, amount, paymentMethod, reference, notes);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @PostMapping("/{id}/partial-payment")
    public ResponseEntity<FeeDTO> recordPartialPayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference) {
        Fee fee = feeService.recordPartialPayment(id, amount, paymentMethod, reference);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @PostMapping("/bulk-payment")
    public ResponseEntity<BulkPaymentResult> recordBulkPayment(
            @RequestParam List<Long> feeIds,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String reference) {
        BulkPaymentResult result = feeService.recordBulkPayment(feeIds, amount, paymentMethod, reference);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<FeeDTO> reversePayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        Fee fee = feeService.reversePayment(id, reason);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    // ==================== Status Operations ====================

    @PostMapping("/{id}/waive")
    public ResponseEntity<FeeDTO> waiveFee(
            @PathVariable Long id,
            @RequestParam String reason) {
        Fee fee = feeService.waiveFee(id, reason);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @PostMapping("/{id}/mark-overdue")
    public ResponseEntity<FeeDTO> markAsOverdue(@PathVariable Long id) {
        Fee fee = feeService.markAsOverdue(id);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<FeeDTO> reopenFee(@PathVariable Long id) {
        Fee fee = feeService.reopenFee(id);
        return ResponseEntity.ok(FeeDTO.fromFee(fee));
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    public ResponseEntity<FeeStatisticsDTO> getFeeStatistics(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        FeeStatisticsDTO stats = feeService.getFeeStatistics(session, term);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        Map<String, Object> stats = feeService.getDetailedStatistics(session, term);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/class-wise")
    public ResponseEntity<List<ClassFeeSummaryDTO>> getClassWiseSummary(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        List<ClassFeeSummaryDTO> summary = feeService.getClassWiseSummary(session, term);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/statistics/monthly")
    public ResponseEntity<List<MonthlyCollectionDTO>> getMonthlyCollectionReport() {
        List<MonthlyCollectionDTO> report = feeService.getMonthlyCollectionReport();
        return ResponseEntity.ok(report);
    }

    // ==================== Defaulters ====================

    @GetMapping("/defaulters")
    public ResponseEntity<List<DefaulterDTO>> getDefaultingStudents(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        List<DefaulterDTO> defaulters = feeService.getDefaultingStudents(session, term);
        return ResponseEntity.ok(defaulters);
    }

    @GetMapping("/defaulters/top")
    public ResponseEntity<List<DefaulterDTO>> getTopDefaulters(
            @RequestParam String session,
            @RequestParam Fee.Term term,
            @RequestParam(defaultValue = "10") int limit) {
        List<DefaulterDTO> defaulters = feeService.getTopDefaulters(session, term, limit);
        return ResponseEntity.ok(defaulters);
    }

    @GetMapping("/defaulters/count")
    public ResponseEntity<Long> countDefaulters(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        long count = feeService.countDefaulters(session, term);
        return ResponseEntity.ok(count);
    }

    // ==================== Reminders ====================

    @PostMapping("/reminders/send")
    public ResponseEntity<ReminderResult> sendFeeReminders(
            @RequestParam String session,
            @RequestParam Fee.Term term,
            @RequestParam(defaultValue = "7") int daysBeforeDue) {
        ReminderResult result = feeService.sendFeeReminders(session, term, daysBeforeDue);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reminders/overdue")
    public ResponseEntity<ReminderResult> sendOverdueReminders() {
        ReminderResult result = feeService.sendOverdueReminders();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/send-reminder")
    public ResponseEntity<ReminderResult> sendSingleReminder(@PathVariable Long id) {
        ReminderResult result = feeService.sendSingleReminder(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reminders/bulk")
    public ResponseEntity<ReminderResult> sendBulkReminders(@RequestParam List<Long> feeIds) {
        ReminderResult result = feeService.sendBulkReminders(feeIds);
        return ResponseEntity.ok(result);
    }

    // ==================== Due Date Operations ====================

    @GetMapping("/overdue")
    public ResponseEntity<List<FeeDTO>> getOverdueFees() {
        List<Fee> fees = feeService.getOverdueFees();
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/overdue/filtered")
    public ResponseEntity<List<FeeDTO>> getOverdueFees(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        List<Fee> fees = feeService.getOverdueFees(session, term);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<FeeDTO>> getUpcomingFees(@RequestParam(defaultValue = "7") int days) {
        List<Fee> fees = feeService.getUpcomingFees(days);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/due-between")
    public ResponseEntity<List<FeeDTO>> getFeesDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<Fee> fees = feeService.getFeesDueBetween(start, end);
        List<FeeDTO> dtos = fees.stream()
                .map(FeeDTO::fromFee)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/due-today/count")
    public ResponseEntity<Long> countFeesDueToday() {
        long count = feeService.countFeesDueToday();
        return ResponseEntity.ok(count);
    }

    // ==================== Payment History ====================

    @GetMapping("/student/{studentId}/payments")
    public ResponseEntity<List<PaymentHistoryDTO>> getStudentPaymentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) String session) {
        List<PaymentHistoryDTO> history = feeService.getPaymentHistory(studentId, session);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentHistoryDTO>> getFeePaymentHistory(@PathVariable Long id) {
        List<PaymentHistoryDTO> history = feeService.getFeePaymentHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/payments/recent")
    public ResponseEntity<List<PaymentHistoryDTO>> getRecentPayments(
            @RequestParam(defaultValue = "10") int limit) {
        List<PaymentHistoryDTO> payments = feeService.getRecentPayments(limit);
        return ResponseEntity.ok(payments);
    }

    // ==================== Reports ====================

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generateFeeReport(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        Map<String, Object> report = feeService.generateFeeReport(session, term);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<Resource> generateFeeReportPdf(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        byte[] reportBytes = feeService.generateFeeReportPdf(session, term);
        ByteArrayResource resource = new ByteArrayResource(reportBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee_report_" + session + "_" + term + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reportBytes.length)
                .body(resource);
    }

    @GetMapping("/report/excel")
    public ResponseEntity<Resource> generateFeeReportExcel(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        byte[] reportBytes = feeService.generateFeeReportExcel(session, term);
        ByteArrayResource resource = new ByteArrayResource(reportBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee_report_" + session + "_" + term + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(reportBytes.length)
                .body(resource);
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<Resource> generateReceipt(@PathVariable Long id) {
        byte[] receiptBytes = feeService.generateReceipt(id);
        ByteArrayResource resource = new ByteArrayResource(receiptBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(receiptBytes.length)
                .body(resource);
    }

    // ==================== Validation ====================

    @GetMapping("/student/{studentId}/has-outstanding")
    public ResponseEntity<Boolean> hasOutstandingFees(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        boolean hasOutstanding = feeService.hasOutstandingFees(studentId, session, term);
        return ResponseEntity.ok(hasOutstanding);
    }

    @GetMapping("/student/{studentId}/outstanding/total")
    public ResponseEntity<Double> getTotalOutstanding(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        Double total = feeService.getTotalOutstanding(studentId, session, term);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/student/{studentId}/outstanding/by-type")
    public ResponseEntity<Map<Fee.FeeType, Double>> getOutstandingByType(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        Map<Fee.FeeType, Double> outstanding = feeService.getOutstandingByType(studentId, session, term);
        return ResponseEntity.ok(outstanding);
    }

    // ==================== Bulk Operations ====================

    @PutMapping("/update-due-dates")
    public ResponseEntity<Integer> updateFeesDueDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate oldDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate) {
        int updated = feeService.updateFeesDueDate(oldDate, newDate);
        return ResponseEntity.ok(updated);
    }

    // ==================== Dashboard ====================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam String session,
            @RequestParam Fee.Term term) {
        Map<String, Object> data = feeService.getDashboardData(session, term);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/payment-trends")
    public ResponseEntity<List<Map<String, Object>>> getPaymentTrends(
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> trends = feeService.getPaymentTrends(months);
        return ResponseEntity.ok(trends);
    }
}