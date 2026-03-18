package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.BulkPaymentResult;
import com.inkFront.schoolManagement.dto.ClassFeeSummaryDTO;
import com.inkFront.schoolManagement.dto.DefaulterDTO;
import com.inkFront.schoolManagement.dto.FeeDTO;
import com.inkFront.schoolManagement.dto.FeeStatisticsDTO;
import com.inkFront.schoolManagement.dto.MonthlyCollectionDTO;
import com.inkFront.schoolManagement.dto.PaymentHistoryDTO;
import com.inkFront.schoolManagement.dto.ReminderResult;
import com.inkFront.schoolManagement.model.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FeeService {

    // Basic CRUD operations
    Fee createFee(FeeDTO feeDTO);
    Fee updateFee(Long id, FeeDTO feeDTO);
    Fee getFee(Long id);
    void deleteFee(Long id);

    // Fee retrieval
    List<Fee> getAllFees(String session, Fee.Term term, Fee.PaymentStatus status);
    Page<Fee> getAllFeesPaginated(String session, Fee.Term term, Fee.PaymentStatus status, Pageable pageable);
    List<Fee> getStudentFees(Long studentId, String session, Fee.Term term);
    List<Fee> getStudentAllFees(Long studentId);

    // Payment operations
    Fee recordPayment(Long feeId, Double amount, String paymentMethod, String reference, String notes);
    Fee recordPartialPayment(Long feeId, Double amount, String paymentMethod, String reference);
    Fee reversePayment(Long feeId, String reason);
    BulkPaymentResult recordBulkPayment(List<Long> feeIds, Double amount, String paymentMethod, String reference);

    // Status operations
    Fee waiveFee(Long feeId, String reason);
    Fee markAsOverdue(Long feeId);
    Fee reopenFee(Long feeId);

    // Statistics and analytics
    FeeStatisticsDTO getFeeStatistics(String session, Fee.Term term);
    Map<String, Object> getDetailedStatistics(String session, Fee.Term term);
    List<ClassFeeSummaryDTO> getClassWiseSummary(String session, Fee.Term term);
    List<MonthlyCollectionDTO> getMonthlyCollectionReport();

    // Defaulter management
    List<DefaulterDTO> getDefaultingStudents(String session, Fee.Term term);
    List<DefaulterDTO> getTopDefaulters(String session, Fee.Term term, int limit);
    long countDefaulters(String session, Fee.Term term);

    // Reminder operations
    ReminderResult sendFeeReminders(String session, Fee.Term term, int daysBeforeDue);
    ReminderResult sendOverdueReminders();
    ReminderResult sendSingleReminder(Long feeId);
    ReminderResult sendBulkReminders(List<Long> feeIds);

    // Due date operations
    List<Fee> getOverdueFees();
    List<Fee> getOverdueFees(String session, Fee.Term term);
    List<Fee> getUpcomingFees(int days);
    List<Fee> getFeesDueBetween(LocalDate start, LocalDate end);
    long countFeesDueToday();

    // Payment history
    List<PaymentHistoryDTO> getPaymentHistory(Long studentId, String session);
    List<PaymentHistoryDTO> getFeePaymentHistory(Long feeId);
    List<PaymentHistoryDTO> getRecentPayments(int limit);

    // Report generation
    Map<String, Object> generateFeeReport(String session, Fee.Term term);
    byte[] generateFeeReportPdf(String session, Fee.Term term);
    byte[] generateFeeReportExcel(String session, Fee.Term term);
    byte[] generateReceipt(Long feeId);

    // Validation
    boolean hasOutstandingFees(Long studentId, String session, Fee.Term term);
    Double getTotalOutstanding(Long studentId, String session, Fee.Term term);
    Map<Fee.FeeType, Double> getOutstandingByType(Long studentId, String session, Fee.Term term);

    // Bulk operations
    List<Fee> createBulkFees(List<FeeDTO> feeDTOs);
    int updateFeesDueDate(LocalDate oldDate, LocalDate newDate);

    // Dashboard data
    Map<String, Object> getDashboardData(String session, Fee.Term term);
    List<Map<String, Object>> getPaymentTrends(int months);
}