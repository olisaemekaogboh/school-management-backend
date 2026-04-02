package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.FeeDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.FeeRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private FeeServiceImpl feeService;

    private Student testStudent;
    private Fee testFee;
    private FeeDTO testFeeDTO;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setParentPhone("08012345678");
        testStudent.setParentName("Mr. John Doe");

        testFee = new Fee();
        testFee.setId(1L);
        testFee.setStudent(testStudent);
        testFee.setSession("2023/2024");
        testFee.setTerm(Fee.Term.FIRST);
        testFee.setFeeType(Fee.FeeType.TUITION);
        testFee.setAmount(50000.0);
        testFee.setPaidAmount(0.0);
        testFee.setBalance(50000.0);
        // Use a date far in the future to ensure it's not in the past
        testFee.setDueDate(LocalDate.now().plusYears(1));
        testFee.setStatus(Fee.PaymentStatus.PENDING);

        testFeeDTO = new FeeDTO();
        testFeeDTO.setStudentId(1L);
        testFeeDTO.setSession("2023/2024");
        testFeeDTO.setTerm(Fee.Term.FIRST);
        testFeeDTO.setFeeType(Fee.FeeType.TUITION);
        testFeeDTO.setAmount(50000.0);
        // Use a date far in the future to ensure it's not in the past
        testFeeDTO.setDueDate(LocalDate.now().plusYears(1));
    }

    @Test
    void createFee_ShouldCreateNewFee() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeRepository.findByStudentAndSessionAndTermAndFeeType(
                any(Student.class), anyString(), any(Fee.Term.class), any(Fee.FeeType.class)))
                .thenReturn(Optional.empty());
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.createFee(testFeeDTO);

        assertNotNull(result);
        assertEquals(50000.0, result.getAmount());
        verify(feeRepository, times(1)).save(any(Fee.class));
    }

    @Test
    void createFee_WithPastDueDate_ShouldThrowException() {
        testFeeDTO.setDueDate(LocalDate.now().minusDays(1));

        assertThrows(BusinessException.class, () -> {
            feeService.createFee(testFeeDTO);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void createFee_WithZeroAmount_ShouldThrowException() {
        testFeeDTO.setAmount(0.0);

        assertThrows(BusinessException.class, () -> {
            feeService.createFee(testFeeDTO);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void createFee_WithNegativeAmount_ShouldThrowException() {
        testFeeDTO.setAmount(-1000.0);

        assertThrows(BusinessException.class, () -> {
            feeService.createFee(testFeeDTO);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void createFee_WithExistingFee_ShouldThrowException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeRepository.findByStudentAndSessionAndTermAndFeeType(
                any(Student.class), anyString(), any(Fee.Term.class), any(Fee.FeeType.class)))
                .thenReturn(Optional.of(testFee));

        // DO NOT stub feeRepository.save() here - it shouldn't be called

        assertThrows(BusinessException.class, () -> {
            feeService.createFee(testFeeDTO);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void updateFee_ShouldUpdateExistingFee() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.updateFee(1L, testFeeDTO);

        assertNotNull(result);
        verify(feeRepository, times(1)).save(testFee);
    }

    @Test
    void updateFee_WithPaidFee_ShouldThrowException() {
        testFee.setStatus(Fee.PaymentStatus.PAID);
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.updateFee(1L, testFeeDTO);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void getFee_ShouldReturnFee() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        Fee result = feeService.getFee(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getFee_WithNonExistentId_ShouldThrowException() {
        when(feeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            feeService.getFee(999L);
        });
    }

    @Test
    void deleteFee_ShouldDeleteFee() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        doNothing().when(feeRepository).delete(testFee);

        feeService.deleteFee(1L);

        verify(feeRepository, times(1)).delete(testFee);
    }

    @Test
    void deleteFee_WithPaidAmount_ShouldThrowException() {
        testFee.setPaidAmount(10000.0);
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.deleteFee(1L);
        });

        verify(feeRepository, never()).delete(any(Fee.class));
    }

    @Test
    void recordPayment_ShouldRecordFullPayment() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.recordPayment(1L, 50000.0, "CASH", "REF123", "Payment");

        assertNotNull(result);
        assertEquals(50000.0, result.getPaidAmount());
        assertEquals(0.0, result.getBalance());
        assertEquals(Fee.PaymentStatus.PAID, result.getStatus());
    }

    @Test
    void recordPayment_ShouldRecordPartialPayment() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.recordPayment(1L, 25000.0, "CASH", "REF123", "Partial payment");

        assertNotNull(result);
        assertEquals(25000.0, result.getPaidAmount());
        assertEquals(25000.0, result.getBalance());
        assertEquals(Fee.PaymentStatus.PARTIAL, result.getStatus());
    }

    @Test
    void recordPayment_WithExcessAmount_ShouldThrowException() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.recordPayment(1L, 60000.0, "CASH", "REF123", "Payment");
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void recordPayment_WithZeroAmount_ShouldThrowException() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.recordPayment(1L, 0.0, "CASH", "REF123", "Payment");
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void reversePayment_ShouldReversePayment() {
        testFee.setPaidAmount(50000.0);
        testFee.setStatus(Fee.PaymentStatus.PAID);
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.reversePayment(1L, "Test reversal");

        assertNotNull(result);
        assertEquals(0.0, result.getPaidAmount());
        assertEquals(50000.0, result.getBalance());
        assertEquals(Fee.PaymentStatus.PENDING, result.getStatus());
    }

    @Test
    void reversePayment_WithNoPayment_ShouldThrowException() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.reversePayment(1L, "Test reversal");
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void waiveFee_ShouldMarkAsWaived() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.waiveFee(1L, "Financial hardship");

        assertNotNull(result);
        assertEquals(Fee.PaymentStatus.WAIVED, result.getStatus());
    }

    @Test
    void waiveFee_WithPaidFee_ShouldThrowException() {
        testFee.setStatus(Fee.PaymentStatus.PAID);
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.waiveFee(1L, "Financial hardship");
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void markAsOverdue_ShouldMarkAsOverdue() {
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(testFee);

        Fee result = feeService.markAsOverdue(1L);

        assertNotNull(result);
        assertEquals(Fee.PaymentStatus.OVERDUE, result.getStatus());
    }

    @Test
    void markAsOverdue_WithPaidFee_ShouldThrowException() {
        testFee.setStatus(Fee.PaymentStatus.PAID);
        when(feeRepository.findById(1L)).thenReturn(Optional.of(testFee));

        assertThrows(BusinessException.class, () -> {
            feeService.markAsOverdue(1L);
        });

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void hasOutstandingFees_ShouldReturnTrue() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeRepository.findByStudentAndSessionAndTerm(any(Student.class), anyString(), any(Fee.Term.class)))
                .thenReturn(Arrays.asList(testFee));

        boolean result = feeService.hasOutstandingFees(1L, "2023/2024", Fee.Term.FIRST);

        assertTrue(result);
    }

    @Test
    void hasOutstandingFees_WithNoFees_ShouldReturnFalse() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeRepository.findByStudentAndSessionAndTerm(any(Student.class), anyString(), any(Fee.Term.class)))
                .thenReturn(Arrays.asList());

        boolean result = feeService.hasOutstandingFees(1L, "2023/2024", Fee.Term.FIRST);

        assertFalse(result);
    }

    @Test
    void getTotalOutstanding_ShouldReturnSum() {
        testFee.setBalance(50000.0);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeRepository.findByStudentAndSessionAndTerm(any(Student.class), anyString(), any(Fee.Term.class)))
                .thenReturn(Arrays.asList(testFee));

        Double result = feeService.getTotalOutstanding(1L, "2023/2024", Fee.Term.FIRST);

        assertEquals(50000.0, result);
    }
}