package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.BulkPaymentResult;
import com.inkFront.schoolManagement.dto.ClassFeeSummaryDTO;
import com.inkFront.schoolManagement.dto.DefaulterDTO;
import com.inkFront.schoolManagement.dto.FeeDTO;
import com.inkFront.schoolManagement.dto.FeeStatisticsDTO;
import com.inkFront.schoolManagement.dto.MonthlyCollectionDTO;
import com.inkFront.schoolManagement.dto.PaymentHistoryDTO;
import com.inkFront.schoolManagement.dto.ReminderResult;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.FeeRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.FeeService;
import com.inkFront.schoolManagement.service.SmsService;
import com.inkFront.schoolManagement.utils.AppConstants;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeeServiceImpl implements FeeService {

    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;
    private final SmsService smsService;

    @Override
    public Fee createFee(FeeDTO feeDTO) {
        log.info("Creating fee for student: {}, amount: {}", feeDTO.getStudentId(), feeDTO.getAmount());

        validateFeeCreation(feeDTO);

        Student student = studentRepository.findById(feeDTO.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + feeDTO.getStudentId()));

        Optional<Fee> existingFee = feeRepository.findByStudentAndSessionAndTermAndFeeType(
                student, feeDTO.getSession(), feeDTO.getTerm(), feeDTO.getFeeType());

        if (existingFee.isPresent()) {
            throw new BusinessException("Fee already exists for this student, session, term, and fee type");
        }

        Fee fee = Fee.builder()
                .student(student)
                .session(feeDTO.getSession())
                .term(feeDTO.getTerm())
                .feeType(feeDTO.getFeeType())
                .description(feeDTO.getDescription())
                .amount(feeDTO.getAmount())
                .paidAmount(0.0)
                .balance(feeDTO.getAmount())
                .dueDate(feeDTO.getDueDate())
                .status(Fee.PaymentStatus.PENDING)
                .reminderCount(0)
                .notes(feeDTO.getNotes() != null ? feeDTO.getNotes() : "")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Fee savedFee = feeRepository.save(fee);
        log.info("Fee created successfully with id: {}", savedFee.getId());
        return savedFee;
    }

    @Override
    public Fee updateFee(Long id, FeeDTO feeDTO) {
        log.info("Updating fee with id: {}", id);

        Fee fee = getFee(id);

        if (fee.getStatus() == Fee.PaymentStatus.PAID) {
            throw new BusinessException("Cannot update a paid fee");
        }

        fee.setAmount(feeDTO.getAmount());
        fee.setDueDate(feeDTO.getDueDate());
        fee.setDescription(feeDTO.getDescription());
        fee.setFeeType(feeDTO.getFeeType());
        fee.setNotes(feeDTO.getNotes());
        fee.setUpdatedAt(LocalDateTime.now());
        fee.setBalance(fee.getAmount() - fee.getPaidAmount());

        Fee updatedFee = feeRepository.save(fee);
        log.info("Fee updated successfully with id: {}", updatedFee.getId());
        return updatedFee;
    }

    @Override
    public Fee getFee(Long id) {
        return feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
    }

    @Override
    public void deleteFee(Long id) {
        log.info("Deleting fee with id: {}", id);

        Fee fee = getFee(id);

        if (fee.getPaidAmount() > 0) {
            throw new BusinessException("Cannot delete fee with existing payments");
        }

        feeRepository.delete(fee);
        log.info("Fee deleted successfully with id: {}", id);
    }

    @Override
    public List<Fee> createBulkFees(List<FeeDTO> feeDTOs) {
        log.info("Creating {} fees in bulk", feeDTOs.size());

        List<Fee> createdFees = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (FeeDTO dto : feeDTOs) {
            try {
                Fee fee = createFee(dto);
                createdFees.add(fee);
            } catch (Exception e) {
                errors.add("Failed to create fee for student " + dto.getStudentId() + ": " + e.getMessage());
                log.error("Error creating fee in bulk: {}", e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Bulk fee creation completed with {} errors: {}", errors.size(), errors);
        }

        return createdFees;
    }

    @Override
    public List<Fee> getAllFees(String session, Fee.Term term, Fee.PaymentStatus status) {
        List<Fee> fees = feeRepository.findAll();

        return fees.stream()
                .filter(f -> session == null || session.equals(f.getSession()))
                .filter(f -> term == null || term.equals(f.getTerm()))
                .filter(f -> status == null || status.equals(f.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<Fee> getAllFeesPaginated(String session, Fee.Term term, Fee.PaymentStatus status, Pageable pageable) {
        List<Fee> filteredFees = getAllFees(session, term, status);

        int start = (int) pageable.getOffset();
        if (start >= filteredFees.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, filteredFees.size());
        }

        int end = Math.min(start + pageable.getPageSize(), filteredFees.size());
        List<Fee> pageContent = filteredFees.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredFees.size());
    }

    @Override
    public List<Fee> getStudentFees(Long studentId, String session, Fee.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return feeRepository.findByStudentAndSessionAndTerm(student, session, term);
    }

    @Override
    public List<Fee> getStudentAllFees(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return feeRepository.findByStudent(student);
    }

    @Override
    public Fee recordPayment(Long feeId, Double amount, String paymentMethod, String reference, String notes) {
        log.info("Recording payment of {} for fee id: {}", amount, feeId);

        Fee fee = getFee(feeId);

        if (amount == null || amount <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }

        if (amount > fee.getBalance()) {
            throw new BusinessException("Payment amount cannot exceed balance of " + fee.getBalance());
        }

        fee.setPaidAmount(fee.getPaidAmount() + amount);
        fee.setBalance(fee.getAmount() - fee.getPaidAmount());
        fee.setPaymentMethod(paymentMethod);
        fee.setPaymentReference(reference);
        fee.setNotes(notes != null ? notes : fee.getNotes());
        fee.setUpdatedAt(LocalDateTime.now());

        if (fee.getBalance() <= 0) {
            fee.setBalance(0.0);
            fee.setPaidDate(LocalDate.now());
            fee.setStatus(Fee.PaymentStatus.PAID);
        } else {
            fee.setStatus(Fee.PaymentStatus.PARTIAL);
        }

        Fee updatedFee = feeRepository.save(fee);
        log.info("Payment recorded successfully for fee id: {}", feeId);

        return updatedFee;
    }

    @Override
    public Fee recordPartialPayment(Long feeId, Double amount, String paymentMethod, String reference) {
        return recordPayment(feeId, amount, paymentMethod, reference, "Partial payment");
    }

    @Override
    public Fee reversePayment(Long feeId, String reason) {
        log.info("Reversing payment for fee id: {} with reason: {}", feeId, reason);

        Fee fee = getFee(feeId);

        if (fee.getPaidAmount() == 0) {
            throw new BusinessException("No payment to reverse");
        }

        Double reversedAmount = fee.getPaidAmount();

        fee.setPaidAmount(0.0);
        fee.setBalance(fee.getAmount());
        fee.setPaymentMethod(null);
        fee.setPaymentReference(null);
        fee.setPaidDate(null);
        fee.setStatus(Fee.PaymentStatus.PENDING);
        fee.setNotes("Payment reversed: " + reason + (fee.getNotes() != null ? " | " + fee.getNotes() : ""));
        fee.setUpdatedAt(LocalDateTime.now());

        Fee updatedFee = feeRepository.save(fee);
        log.info("Payment of {} reversed successfully for fee id: {}", reversedAmount, feeId);

        return updatedFee;
    }

    @Override
    public BulkPaymentResult recordBulkPayment(List<Long> feeIds, Double amount, String paymentMethod, String reference) {
        log.info("Recording bulk payment for {} fees with total amount: {}", feeIds.size(), amount);

        BulkPaymentResult result = BulkPaymentResult.builder()
                .totalProcessed(feeIds.size())
                .successful(0)
                .failed(0)
                .totalAmount(0.0)
                .bulkReference(reference != null ? reference : "BULK-" + System.currentTimeMillis())
                .build();

        Double remainingAmount = amount;

        for (Long feeId : feeIds) {
            try {
                Fee fee = getFee(feeId);
                Double paymentAmount = Math.min(remainingAmount, fee.getBalance());

                if (paymentAmount > 0) {
                    recordPayment(feeId, paymentAmount, paymentMethod, result.getBulkReference(), "Bulk payment");
                    result.getSuccessfulIds().add(feeId);
                    result.setSuccessful(result.getSuccessful() + 1);
                    result.setTotalAmount(result.getTotalAmount() + paymentAmount);
                    remainingAmount -= paymentAmount;
                }

                if (remainingAmount <= 0) {
                    break;
                }

            } catch (Exception e) {
                log.error("Error processing bulk payment for fee {}: {}", feeId, e.getMessage());
                result.getFailedIds().add(feeId);
                result.setFailed(result.getFailed() + 1);
                result.getErrors().add("Fee " + feeId + ": " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public Fee waiveFee(Long feeId, String reason) {
        log.info("Waiving fee id: {} with reason: {}", feeId, reason);

        Fee fee = getFee(feeId);

        if (fee.getStatus() == Fee.PaymentStatus.PAID) {
            throw new BusinessException("Cannot waive a paid fee");
        }

        fee.setStatus(Fee.PaymentStatus.WAIVED);
        fee.setNotes("Fee waived: " + reason + (fee.getNotes() != null ? " | " + fee.getNotes() : ""));
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    public Fee markAsOverdue(Long feeId) {
        log.info("Marking fee as overdue with id: {}", feeId);

        Fee fee = getFee(feeId);

        if (fee.getStatus() == Fee.PaymentStatus.PAID) {
            throw new BusinessException("Cannot mark a paid fee as overdue");
        }

        fee.setStatus(Fee.PaymentStatus.OVERDUE);
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    public Fee reopenFee(Long feeId) {
        log.info("Reopening fee with id: {}", feeId);

        Fee fee = getFee(feeId);

        if (fee.getStatus() != Fee.PaymentStatus.WAIVED && fee.getStatus() != Fee.PaymentStatus.OVERDUE) {
            throw new BusinessException("Only waived or overdue fees can be reopened");
        }

        fee.setStatus(fee.getBalance() > 0 ? Fee.PaymentStatus.PENDING : Fee.PaymentStatus.PAID);
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    public FeeStatisticsDTO getFeeStatistics(String session, Fee.Term term) {
        Double totalExpected = Optional.ofNullable(
                feeRepository.getTotalExpectedBySessionAndTerm(session, term)).orElse(0.0);
        Double totalCollected = Optional.ofNullable(
                feeRepository.getTotalCollectedBySessionAndTerm(session, term)).orElse(0.0);
        Double totalOutstanding = Optional.ofNullable(
                feeRepository.getTotalOutstandingBySessionAndTerm(session, term)).orElse(0.0);
        Long totalFees = Optional.ofNullable(
                feeRepository.getTotalFeesCountBySessionAndTerm(session, term)).orElse(0L);
        Long totalStudents = Optional.ofNullable(
                feeRepository.getTotalStudentsWithFeesBySessionAndTerm(session, term)).orElse(0L);

        List<Object[]> statusCounts = feeRepository.countFeesByStatus(session, term);
        Map<String, Long> statusBreakdown = new HashMap<>();
        long paidCount = 0, partialCount = 0, pendingCount = 0, overdueCount = 0, waivedCount = 0;

        for (Object[] row : statusCounts) {
            Fee.PaymentStatus status = (Fee.PaymentStatus) row[0];
            Long count = (Long) row[1];
            statusBreakdown.put(status.name(), count);

            switch (status) {
                case PAID -> paidCount = count;
                case PARTIAL -> partialCount = count;
                case PENDING -> pendingCount = count;
                case OVERDUE -> overdueCount = count;
                case WAIVED -> waivedCount = count;
            }
        }

        return FeeStatisticsDTO.builder()
                .totalExpected(totalExpected)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .totalFees(totalFees)
                .totalStudents(totalStudents)
                .paidCount(paidCount)
                .partialCount(partialCount)
                .pendingCount(pendingCount)
                .overdueCount(overdueCount)
                .waivedCount(waivedCount)
                .collectionRate(totalExpected > 0 ? (totalCollected / totalExpected * 100) : 0)
                .outstandingRate(totalExpected > 0 ? (totalOutstanding / totalExpected * 100) : 0)
                .statusBreakdown(statusBreakdown)
                .typeBreakdown(new HashMap<>())
                .build();
    }

    @Override
    public Map<String, Object> getDetailedStatistics(String session, Fee.Term term) {
        Map<String, Object> detailedStats = new HashMap<>();
        detailedStats.put("basic", getFeeStatistics(session, term));
        detailedStats.put("classWise", getClassWiseSummary(session, term));
        detailedStats.put("defaulterCount", countDefaulters(session, term));
        detailedStats.put("monthlyTrends", getMonthlyCollectionReport().stream().limit(6).collect(Collectors.toList()));
        detailedStats.put("dueToday", countFeesDueToday());

        List<Fee> fees = feeRepository.findBySessionAndTerm(session, term);
        detailedStats.put("averageFeeAmount", fees.stream().mapToDouble(Fee::getAmount).average().orElse(0.0));
        detailedStats.put("highestFee", fees.stream().mapToDouble(Fee::getAmount).max().orElse(0.0));

        return detailedStats;
    }

    @Override
    public List<ClassFeeSummaryDTO> getClassWiseSummary(String session, Fee.Term term) {
        List<Object[]> results = feeRepository.getClassWiseSummary(session, term);
        List<ClassFeeSummaryDTO> summaries = new ArrayList<>();

        for (Object[] row : results) {
            String className = (String) row[0];
            Long totalStudents = (Long) row[1];
            Double totalExpected = row[2] != null ? (Double) row[2] : 0.0;
            Double totalCollected = row[3] != null ? (Double) row[3] : 0.0;
            Double totalOutstanding = totalExpected - totalCollected;

            List<Fee> classFees = feeRepository.findBySessionAndTerm(session, term).stream()
                    .filter(f -> f.getStudent() != null
                            && f.getStudent().getStudentClass() != null
                            && f.getStudent().getStudentClass().equals(className))
                    .collect(Collectors.toList());

            long fullyPaid = classFees.stream().filter(f -> f.getStatus() == Fee.PaymentStatus.PAID).count();
            long partiallyPaid = classFees.stream().filter(f -> f.getStatus() == Fee.PaymentStatus.PARTIAL).count();
            long notPaid = classFees.stream().filter(f -> f.getStatus() == Fee.PaymentStatus.PENDING).count();
            long overdue = classFees.stream().filter(f -> f.getStatus() == Fee.PaymentStatus.OVERDUE).count();

            summaries.add(ClassFeeSummaryDTO.builder()
                    .studentClass(className)
                    .totalStudents(totalStudents.intValue())
                    .studentsWithFees((int) classFees.stream().map(f -> f.getStudent().getId()).distinct().count())
                    .fullyPaid((int) fullyPaid)
                    .partiallyPaid((int) partiallyPaid)
                    .notPaid((int) notPaid)
                    .overdue((int) overdue)
                    .totalExpected(totalExpected)
                    .totalCollected(totalCollected)
                    .totalOutstanding(totalOutstanding)
                    .collectionPercentage(totalExpected > 0 ? (totalCollected / totalExpected * 100) : 0)
                    .averagePerStudent(totalStudents > 0 ? totalExpected / totalStudents : 0)
                    .build());
        }

        return summaries;
    }

    @Override
    public List<MonthlyCollectionDTO> getMonthlyCollectionReport() {
        List<Object[]> results = feeRepository.getMonthlyCollectionReport();
        List<MonthlyCollectionDTO> monthlyData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM");

        for (Object[] row : results) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            Double amount = row[2] != null ? (Double) row[2] : 0.0;

            LocalDate date = LocalDate.of(year, month, 1);

            monthlyData.add(MonthlyCollectionDTO.builder()
                    .year(year)
                    .month(month)
                    .monthName(date.format(formatter))
                    .amount(amount)
                    .build());
        }

        return monthlyData;
    }

    @Override
    public List<DefaulterDTO> getDefaultingStudents(String session, Fee.Term term) {
        List<Object[]> results = feeRepository.getStudentsWithOutstandingBalance(session, term);
        List<DefaulterDTO> defaulters = new ArrayList<>();

        for (Object[] result : results) {
            Student student = (Student) result[0];
            Double outstanding = (Double) result[1];

            List<Fee> studentFees = feeRepository.findByStudentAndSessionAndTerm(student, session, term);

            List<Fee> overdueFees = studentFees.stream()
                    .filter(f -> f.getStatus() == Fee.PaymentStatus.OVERDUE)
                    .collect(Collectors.toList());

            if (!overdueFees.isEmpty()) {
                Fee mostUrgent = overdueFees.stream()
                        .min(Comparator.comparing(Fee::getDueDate))
                        .orElse(null);

                defaulters.add(DefaulterDTO.builder()
                        .studentId(student.getId())
                        .studentName(student.getFirstName() + " " + student.getLastName())
                        .admissionNumber(student.getAdmissionNumber())
                        .studentClass(student.getStudentClass())
                        .classArm(student.getClassArm())
                        .parentName(student.getParentName())
                        .parentPhone(student.getParentPhone())
                        .outstandingBalance(outstanding)
                        .overdueFees(overdueFees.size())
                        .totalFees(studentFees.size())
                        .fees(studentFees.stream().map(FeeDTO::fromFee).collect(Collectors.toList()))
                        .mostUrgentFeeType(mostUrgent != null ? mostUrgent.getFeeType().name() : null)
                        .earliestDueDate(mostUrgent != null ? mostUrgent.getDueDate() : null)
                        .build());
            }
        }

        defaulters.sort((a, b) -> Double.compare(b.getOutstandingBalance(), a.getOutstandingBalance()));
        return defaulters;
    }

    @Override
    public List<DefaulterDTO> getTopDefaulters(String session, Fee.Term term, int limit) {
        return getDefaultingStudents(session, term).stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public long countDefaulters(String session, Fee.Term term) {
        return feeRepository.getStudentsWithOutstandingBalance(session, term).size();
    }

    @Override
    public ReminderResult sendFeeReminders(String session, Fee.Term term, int daysBeforeDue) {
        ReminderResult result = ReminderResult.builder()
                .totalSent(0)
                .successful(0)
                .failed(0)
                .build();

        LocalDate start = LocalDate.now().plusDays(daysBeforeDue - 1);
        LocalDate end = LocalDate.now().plusDays(daysBeforeDue);

        List<Fee> upcomingFees = feeRepository.findUpcomingFeesBySessionAndTerm(start, end, session, term);

        for (Fee fee : upcomingFees) {
            if (fee.getStatus() != Fee.PaymentStatus.PAID && fee.getReminderCount() < AppConstants.MAX_REMINDER_COUNT) {
                try {
                    sendFeeReminderSms(fee, daysBeforeDue);
                    fee.setReminderCount(fee.getReminderCount() + 1);
                    fee.setLastReminderSent(LocalDate.now());
                    feeRepository.save(fee);

                    result.setSuccessful(result.getSuccessful() + 1);
                    result.getSuccessfulNumbers().add(fee.getStudent().getParentPhone());
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailedNumbers().add(fee.getStudent().getParentPhone());
                    result.getErrors().add(e.getMessage());
                }
                result.setTotalSent(result.getTotalSent() + 1);
            }
        }

        result.setMessage(String.format("Sent %d reminders, %d successful, %d failed",
                result.getTotalSent(), result.getSuccessful(), result.getFailed()));

        return result;
    }

    @Override
    public ReminderResult sendOverdueReminders() {
        ReminderResult result = ReminderResult.builder()
                .totalSent(0)
                .successful(0)
                .failed(0)
                .build();

        List<Fee> overdueFees = feeRepository.findFeesNeedingReminders(AppConstants.MAX_REMINDER_COUNT);

        for (Fee fee : overdueFees) {
            if (fee.getStatus() == Fee.PaymentStatus.OVERDUE) {
                try {
                    sendOverdueReminderSms(fee);
                    fee.setReminderCount(fee.getReminderCount() + 1);
                    fee.setLastReminderSent(LocalDate.now());
                    feeRepository.save(fee);

                    result.setSuccessful(result.getSuccessful() + 1);
                    result.getSuccessfulNumbers().add(fee.getStudent().getParentPhone());
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailedNumbers().add(fee.getStudent().getParentPhone());
                    result.getErrors().add(e.getMessage());
                }
                result.setTotalSent(result.getTotalSent() + 1);
            }
        }

        result.setMessage(String.format("Sent %d overdue reminders, %d successful, %d failed",
                result.getTotalSent(), result.getSuccessful(), result.getFailed()));

        return result;
    }

    @Override
    public ReminderResult sendSingleReminder(Long feeId) {
        ReminderResult result = ReminderResult.builder()
                .totalSent(1)
                .successful(0)
                .failed(0)
                .build();

        try {
            Fee fee = getFee(feeId);
            sendFeeReminderSms(fee, 7);

            fee.setReminderCount(fee.getReminderCount() + 1);
            fee.setLastReminderSent(LocalDate.now());
            feeRepository.save(fee);

            result.setSuccessful(1);
            result.getSuccessfulNumbers().add(fee.getStudent().getParentPhone());
            result.setMessage("Reminder sent successfully");
        } catch (Exception e) {
            result.setFailed(1);
            result.getErrors().add(e.getMessage());
            result.setMessage("Failed to send reminder: " + e.getMessage());
        }

        return result;
    }

    @Override
    public ReminderResult sendBulkReminders(List<Long> feeIds) {
        ReminderResult result = ReminderResult.builder()
                .totalSent(feeIds.size())
                .successful(0)
                .failed(0)
                .build();

        for (Long feeId : feeIds) {
            try {
                ReminderResult singleResult = sendSingleReminder(feeId);
                if (singleResult.getSuccessful() > 0) {
                    result.setSuccessful(result.getSuccessful() + 1);
                } else {
                    result.setFailed(result.getFailed() + 1);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getErrors().add("Fee " + feeId + ": " + e.getMessage());
            }
        }

        result.setMessage(String.format("Sent %d reminders, %d successful, %d failed",
                result.getTotalSent(), result.getSuccessful(), result.getFailed()));

        return result;
    }

    @Override
    public List<Fee> getOverdueFees() {
        return feeRepository.findOverdueFees(LocalDate.now());
    }

    @Override
    public List<Fee> getOverdueFees(String session, Fee.Term term) {
        return feeRepository.findBySessionAndTerm(session, term).stream()
                .filter(f -> f.getStatus() == Fee.PaymentStatus.OVERDUE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Fee> getUpcomingFees(int days) {
        LocalDate now = LocalDate.now();
        LocalDate end = now.plusDays(days);
        return feeRepository.findUpcomingFees(now, end);
    }

    @Override
    public List<Fee> getFeesDueBetween(LocalDate start, LocalDate end) {
        return feeRepository.findUpcomingFees(start, end);
    }

    @Override
    public long countFeesDueToday() {
        LocalDate today = LocalDate.now();
        return feeRepository.countDueFeesInRange(today, today);
    }

    @Override
    public List<PaymentHistoryDTO> getPaymentHistory(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<Fee> paidFees = feeRepository.findPaymentHistoryByStudent(student);

        return paidFees.stream()
                .filter(f -> session == null || session.equals(f.getSession()))
                .map(f -> PaymentHistoryDTO.builder()
                        .id(f.getId())
                        .feeId(f.getId())
                        .feeType(f.getFeeType().name())
                        .description(f.getDescription())
                        .amount(f.getAmount())
                        .paidAmount(f.getPaidAmount())
                        .balance(f.getBalance())
                        .paymentDate(f.getPaidDate())
                        .paymentMethod(f.getPaymentMethod())
                        .paymentReference(f.getPaymentReference())
                        .term(f.getTerm().name())
                        .session(f.getSession())
                        .studentName(f.getStudent().getFirstName() + " " + f.getStudent().getLastName())
                        .admissionNumber(f.getStudent().getAdmissionNumber())
                        .build())
                .sorted((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentHistoryDTO> getFeePaymentHistory(Long feeId) {
        Fee fee = getFee(feeId);

        return Collections.singletonList(PaymentHistoryDTO.builder()
                .id(feeId)
                .feeId(feeId)
                .feeType(fee.getFeeType().name())
                .description(fee.getDescription())
                .amount(fee.getAmount())
                .paidAmount(fee.getPaidAmount())
                .balance(fee.getBalance())
                .paymentDate(fee.getPaidDate())
                .paymentMethod(fee.getPaymentMethod())
                .paymentReference(fee.getPaymentReference())
                .term(fee.getTerm().name())
                .session(fee.getSession())
                .studentName(fee.getStudent().getFirstName() + " " + fee.getStudent().getLastName())
                .admissionNumber(fee.getStudent().getAdmissionNumber())
                .build());
    }

    @Override
    public List<PaymentHistoryDTO> getRecentPayments(int limit) {
        return feeRepository.findAll().stream()
                .filter(f -> f.getPaidDate() != null)
                .sorted((a, b) -> b.getPaidDate().compareTo(a.getPaidDate()))
                .limit(limit)
                .map(f -> PaymentHistoryDTO.builder()
                        .id(f.getId())
                        .feeId(f.getId())
                        .feeType(f.getFeeType().name())
                        .description(f.getDescription())
                        .amount(f.getAmount())
                        .paidAmount(f.getPaidAmount())
                        .balance(f.getBalance())
                        .paymentDate(f.getPaidDate())
                        .paymentMethod(f.getPaymentMethod())
                        .paymentReference(f.getPaymentReference())
                        .term(f.getTerm().name())
                        .session(f.getSession())
                        .studentName(f.getStudent().getFirstName() + " " + f.getStudent().getLastName())
                        .admissionNumber(f.getStudent().getAdmissionNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> generateFeeReport(String session, Fee.Term term) {
        Map<String, Object> report = new HashMap<>();

        List<Fee> fees = feeRepository.findBySessionAndTerm(session, term);

        double totalExpected = fees.stream().mapToDouble(Fee::getAmount).sum();
        double totalPaid = fees.stream()
                .filter(f -> f.getStatus() == Fee.PaymentStatus.PAID)
                .mapToDouble(Fee::getAmount)
                .sum();
        double totalCollected = fees.stream().mapToDouble(Fee::getPaidAmount).sum();
        double totalOutstanding = fees.stream()
                .filter(f -> f.getStatus() != Fee.PaymentStatus.PAID)
                .mapToDouble(Fee::getBalance)
                .sum();
        long totalStudents = fees.stream().map(Fee::getStudent).distinct().count();
        long paidStudents = fees.stream()
                .filter(f -> f.getStatus() == Fee.PaymentStatus.PAID)
                .map(Fee::getStudent)
                .distinct()
                .count();

        Map<Fee.PaymentStatus, Long> statusCount = fees.stream()
                .collect(Collectors.groupingBy(Fee::getStatus, Collectors.counting()));

        Map<Fee.FeeType, Double> typeTotal = fees.stream()
                .collect(Collectors.groupingBy(Fee::getFeeType, Collectors.summingDouble(Fee::getAmount)));

        report.put("session", session);
        report.put("term", term);
        report.put("totalExpected", totalExpected);
        report.put("totalPaid", totalPaid);
        report.put("totalCollected", totalCollected);
        report.put("totalOutstanding", totalOutstanding);
        report.put("totalStudents", totalStudents);
        report.put("paidStudents", paidStudents);
        report.put("defaultingStudents", totalStudents - paidStudents);
        report.put("collectionRate", totalExpected > 0 ? (totalCollected / totalExpected * 100) : 0);
        report.put("statusBreakdown", statusCount);
        report.put("typeBreakdown", typeTotal);
        report.put("fees", fees.stream().map(FeeDTO::fromFee).collect(Collectors.toList()));
        report.put("defaulters", getDefaultingStudents(session, term));
        report.put("generatedAt", LocalDateTime.now().toString());

        return report;
    }

    @Override
    public byte[] generateFeeReportPdf(String session, Fee.Term term) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Fee Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Session: " + session + " | Term: " + term);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);
            document.add(new Paragraph(" "));

            Map<String, Object> reportData = generateFeeReport(session, term);
            @SuppressWarnings("unchecked")
            List<FeeDTO> fees = (List<FeeDTO>) reportData.get("fees");

            if (fees != null && !fees.isEmpty()) {
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);

                String[] headers = {"Student", "Fee Type", "Amount (₦)", "Paid (₦)", "Balance (₦)", "Due Date", "Status"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setBackgroundColor(new Color(240, 240, 240));
                    table.addCell(cell);
                }

                for (FeeDTO fee : fees) {
                    String feeType = fee.getFeeType() != null ? fee.getFeeType().toString() : "";
                    String status = fee.getStatus() != null ? fee.getStatus().toString() : "";
                    String studentName = fee.getStudentName() != null ? fee.getStudentName() : "";
                    String dueDate = fee.getDueDate() != null ? fee.getDueDate().toString() : "";

                    table.addCell(studentName);
                    table.addCell(feeType);
                    table.addCell("₦" + String.format("%,.2f", fee.getAmount()));
                    table.addCell("₦" + String.format("%,.2f", fee.getPaidAmount()));
                    table.addCell("₦" + String.format("%,.2f", fee.getBalance()));
                    table.addCell(dueDate);
                    table.addCell(status);
                }

                document.add(table);
            } else {
                document.add(new Paragraph("No fee records found for the selected period."));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate PDF report: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateFeeReportExcel(String session, Fee.Term term) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Fee Report");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Map<String, Object> reportData = generateFeeReport(session, term);
            @SuppressWarnings("unchecked")
            List<FeeDTO> fees = (List<FeeDTO>) reportData.get("fees");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("FEE REPORT - " + session + " " + term);

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            Row summaryRow1 = sheet.createRow(3);
            summaryRow1.createCell(0).setCellValue("SUMMARY");
            summaryRow1.getCell(0).setCellStyle(titleStyle);

            Row totalExpectedRow = sheet.createRow(4);
            totalExpectedRow.createCell(0).setCellValue("Total Expected:");
            totalExpectedRow.createCell(1).setCellValue((Double) reportData.get("totalExpected"));

            Row totalCollectedRow = sheet.createRow(5);
            totalCollectedRow.createCell(0).setCellValue("Total Collected:");
            totalCollectedRow.createCell(1).setCellValue((Double) reportData.get("totalCollected"));

            Row totalOutstandingRow = sheet.createRow(6);
            totalOutstandingRow.createCell(0).setCellValue("Total Outstanding:");
            totalOutstandingRow.createCell(1).setCellValue((Double) reportData.get("totalOutstanding"));

            Row collectionRateRow = sheet.createRow(7);
            collectionRateRow.createCell(0).setCellValue("Collection Rate:");
            collectionRateRow.createCell(1).setCellValue((Double) reportData.get("collectionRate") + "%");

            Row headerRow = sheet.createRow(9);
            String[] columns = {
                    "Student Name", "Admission No", "Class", "Fee Type", "Description",
                    "Amount (₦)", "Paid (₦)", "Balance (₦)", "Due Date", "Status", "Payment Method"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 10;
            if (fees != null) {
                for (FeeDTO fee : fees) {
                    Row row = sheet.createRow(rowNum++);

                    String feeType = fee.getFeeType() != null ? fee.getFeeType().toString() : "";
                    String status = fee.getStatus() != null ? fee.getStatus().toString() : "";
                    String studentClass = fee.getStudentClass() != null ? fee.getStudentClass() : "";
                    String classArm = fee.getClassArm() != null ? fee.getClassArm() : "";

                    row.createCell(0).setCellValue(fee.getStudentName() != null ? fee.getStudentName() : "");
                    row.createCell(1).setCellValue(fee.getAdmissionNumber() != null ? fee.getAdmissionNumber() : "");
                    row.createCell(2).setCellValue((studentClass + " " + classArm).trim());
                    row.createCell(3).setCellValue(feeType);
                    row.createCell(4).setCellValue(fee.getDescription() != null ? fee.getDescription() : "");
                    row.createCell(5).setCellValue(fee.getAmount());
                    row.createCell(6).setCellValue(fee.getPaidAmount());
                    row.createCell(7).setCellValue(fee.getBalance());
                    row.createCell(8).setCellValue(fee.getDueDate() != null ? fee.getDueDate().toString() : "");
                    row.createCell(9).setCellValue(status);
                    row.createCell(10).setCellValue(fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "");
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel report: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateReceipt(Long feeId) {
        Fee fee = getFee(feeId);

        if (fee.getStatus() != Fee.PaymentStatus.PAID) {
            throw new BusinessException("Receipt can only be generated for paid fees");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5);
            PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph schoolName = new Paragraph("SCHOOL NAME", titleFont);
            schoolName.setAlignment(Element.ALIGN_CENTER);
            document.add(schoolName);

            com.lowagie.text.Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph receiptTitle = new Paragraph("PAYMENT RECEIPT", subtitleFont);
            receiptTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(receiptTitle);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(10f);

            addTableRow(infoTable, "Receipt No:", fee.getPaymentReference() != null ? fee.getPaymentReference() : "RCP-" + fee.getId());
            addTableRow(infoTable, "Date:", fee.getPaidDate() != null ? fee.getPaidDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            addTableRow(infoTable, "Student Name:", fee.getStudent().getFirstName() + " " + fee.getStudent().getLastName());
            addTableRow(infoTable, "Admission No:", fee.getStudent().getAdmissionNumber());
            addTableRow(infoTable, "Class:", fee.getStudent().getStudentClass() + " " + (fee.getStudent().getClassArm() != null ? fee.getStudent().getClassArm() : ""));
            addTableRow(infoTable, "Session/Term:", fee.getSession() + " - " + fee.getTerm());

            document.add(infoTable);
            document.add(new Paragraph(" "));

            PdfPTable feeTable = new PdfPTable(2);
            feeTable.setWidthPercentage(100);
            feeTable.setWidths(new float[]{70, 30});

            PdfPCell cell1 = new PdfPCell(new Phrase("Description", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell1.setBackgroundColor(new Color(240, 240, 240));
            feeTable.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase("Amount (₦)", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setBackgroundColor(new Color(240, 240, 240));
            feeTable.addCell(cell2);

            feeTable.addCell(fee.getFeeType() + (fee.getDescription() != null ? " - " + fee.getDescription() : ""));
            feeTable.addCell(String.format("%,.2f", fee.getAmount()));

            feeTable.addCell("Payment Received");
            feeTable.addCell(String.format("%,.2f", fee.getPaidAmount()));

            feeTable.addCell("Balance");
            feeTable.addCell(String.format("%,.2f", fee.getBalance()));

            document.add(feeTable);
            document.add(new Paragraph(" "));

            Paragraph total = new Paragraph("Total Paid: ₦" + String.format("%,.2f", fee.getPaidAmount()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph("Payment Method: " + (fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "N/A")));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Paragraph thankYou = new Paragraph("Thank you for your payment!",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            thankYou.setAlignment(Element.ALIGN_CENTER);
            document.add(thankYou);

            Paragraph note = new Paragraph("This is a computer generated receipt");
            note.setAlignment(Element.ALIGN_CENTER);
            document.add(note);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating receipt: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate receipt: " + e.getMessage());
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    @Override
    public boolean hasOutstandingFees(Long studentId, String session, Fee.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return feeRepository.findByStudentAndSessionAndTerm(student, session, term).stream()
                .anyMatch(f -> f.getStatus() != Fee.PaymentStatus.PAID);
    }

    @Override
    public Double getTotalOutstanding(Long studentId, String session, Fee.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return feeRepository.findByStudentAndSessionAndTerm(student, session, term).stream()
                .filter(f -> f.getStatus() != Fee.PaymentStatus.PAID)
                .mapToDouble(Fee::getBalance)
                .sum();
    }

    @Override
    public Map<Fee.FeeType, Double> getOutstandingByType(Long studentId, String session, Fee.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return feeRepository.findByStudentAndSessionAndTerm(student, session, term).stream()
                .filter(f -> f.getStatus() != Fee.PaymentStatus.PAID)
                .collect(Collectors.groupingBy(Fee::getFeeType, Collectors.summingDouble(Fee::getBalance)));
    }

    @Override
    public int updateFeesDueDate(LocalDate oldDate, LocalDate newDate) {
        List<Fee> fees = feeRepository.findUpcomingFees(oldDate, oldDate);

        for (Fee fee : fees) {
            fee.setDueDate(newDate);
            fee.setUpdatedAt(LocalDateTime.now());
        }

        feeRepository.saveAll(fees);
        return fees.size();
    }

    @Override
    public Map<String, Object> getDashboardData(String session, Fee.Term term) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("statistics", getFeeStatistics(session, term));
        dashboard.put("recentPayments", getRecentPayments(10));
        dashboard.put("topDefaulters", getTopDefaulters(session, term, 5));
        dashboard.put("classSummary", getClassWiseSummary(session, term));
        dashboard.put("upcomingFees", getUpcomingFees(7).stream().map(FeeDTO::fromFee).collect(Collectors.toList()));
        dashboard.put("overdueCount", feeRepository.countOverdueFeesBySessionAndTerm(session, term));
        dashboard.put("collectionTrend", getMonthlyCollectionReport().stream().limit(6).collect(Collectors.toList()));
        return dashboard;
    }

    @Override
    public List<Map<String, Object>> getPaymentTrends(int months) {
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        for (int i = 0; i < months; i++) {
            LocalDate monthStart = startDate.plusMonths(i);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));

            List<Fee> paymentsInMonth = feeRepository.findPaymentsInDateRange(monthStart, monthEnd);

            double totalCollected = paymentsInMonth.stream()
                    .mapToDouble(Fee::getPaidAmount)
                    .sum();

            monthData.put("amount", totalCollected);
            monthData.put("count", paymentsInMonth.size());

            trends.add(monthData);
        }

        return trends;
    }

    private void validateFeeCreation(FeeDTO feeDTO) {
        if (feeDTO.getDueDate() == null) {
            throw new BusinessException("Due date is required");
        }

        if (feeDTO.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Due date cannot be in the past");
        }

        if (feeDTO.getAmount() == null || feeDTO.getAmount() <= 0) {
            throw new BusinessException("Amount must be positive");
        }
    }

    private void sendFeeReminderSms(Fee fee, int daysBeforeDue) {
        Student student = fee.getStudent();
        String parentPhone = student.getParentPhone();

        if (parentPhone != null && !parentPhone.isEmpty()) {
            String message = String.format(
                    "Dear %s, this is a reminder that your ward %s's %s fee of ₦%.2f is due in %d days on %s. Current balance: ₦%.2f. Please make payment to avoid penalties.",
                    student.getParentName(),
                    student.getFirstName() + " " + student.getLastName(),
                    fee.getFeeType().getDisplayName(),
                    fee.getAmount(),
                    daysBeforeDue,
                    fee.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fee.getBalance()
            );

            smsService.sendSms(parentPhone, message);
            log.info("Sent fee reminder to {} for student {}", parentPhone, student.getId());
        }
    }

    private void sendOverdueReminderSms(Fee fee) {
        Student student = fee.getStudent();
        String parentPhone = student.getParentPhone();

        if (parentPhone != null && !parentPhone.isEmpty()) {
            long daysOverdue = LocalDate.now().toEpochDay() - fee.getDueDate().toEpochDay();

            String message = String.format(
                    "Dear %s, URGENT: Your ward %s's %s fee of ₦%.2f is %d days overdue. Please clear the outstanding balance of ₦%.2f immediately to avoid further action.",
                    student.getParentName(),
                    student.getFirstName() + " " + student.getLastName(),
                    fee.getFeeType().getDisplayName(),
                    fee.getAmount(),
                    daysOverdue,
                    fee.getBalance()
            );

            smsService.sendSms(parentPhone, message);
            log.info("Sent overdue reminder to {} for student {}", parentPhone, student.getId());
        }
    }
}