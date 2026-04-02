package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.DefaulterDTO;
import com.inkFront.schoolManagement.dto.FeeDTO;
import com.inkFront.schoolManagement.dto.FeeStatisticsDTO;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.FeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FeeService feeService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private FeeController feeController;

    private ObjectMapper objectMapper;
    private FeeDTO testFeeDTO;
    private Fee testFee;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testAdminUser = new User();
        testAdminUser.setId(1L);
        testAdminUser.setRole(User.Role.ADMIN);

        testFeeDTO = new FeeDTO();
        testFeeDTO.setId(1L);
        testFeeDTO.setFeeType(Fee.FeeType.TUITION);
        testFeeDTO.setAmount(50000.0);
        testFeeDTO.setSession("2023/2024");
        testFeeDTO.setTerm(Fee.Term.FIRST);
        testFeeDTO.setDescription("Tuition Fee");
        testFeeDTO.setDueDate(LocalDate.now().plusDays(30));
        testFeeDTO.setStudentId(1L);
        testFeeDTO.setPaidAmount(0.0);
        testFeeDTO.setBalance(50000.0);
        testFeeDTO.setStatus(Fee.PaymentStatus.PENDING);

        Student student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setAdmissionNumber("STU001");

        testFee = new Fee();
        testFee.setId(1L);
        testFee.setStudent(student);
        testFee.setFeeType(Fee.FeeType.TUITION);
        testFee.setAmount(50000.0);
        testFee.setPaidAmount(0.0);
        testFee.setBalance(50000.0);
        testFee.setSession("2023/2024");
        testFee.setTerm(Fee.Term.FIRST);
        testFee.setDescription("Tuition Fee");
        testFee.setDueDate(LocalDate.now().plusDays(30));
        testFee.setStatus(Fee.PaymentStatus.PENDING);
        testFee.setPaymentMethod("CASH");
        testFee.setPaymentReference("REF123");
        testFee.setNotes("Test note");
    }

    @Test
    void createFee_ShouldReturnCreatedFee() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.createFee(any(FeeDTO.class))).thenReturn(testFee);

        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFeeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(50000.0));

        verify(feeService, times(1)).createFee(any(FeeDTO.class));
    }

    @Test
    void createBulkFees_ShouldReturnCreatedFees() throws Exception {
        List<FeeDTO> feeDTOs = Arrays.asList(testFeeDTO, testFeeDTO);
        List<Fee> fees = Arrays.asList(testFee, testFee);

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.createBulkFees(anyList())).thenReturn(fees);

        mockMvc.perform(post("/api/fees/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feeDTOs)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        verify(feeService, times(1)).createBulkFees(anyList());
    }

    @Test
    void updateFee_ShouldReturnUpdatedFee() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.updateFee(eq(1L), any(FeeDTO.class))).thenReturn(testFee);

        mockMvc.perform(put("/api/fees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFeeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(feeService, times(1)).updateFee(eq(1L), any(FeeDTO.class));
    }

    @Test
    void getFee_ShouldReturnFee() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getFee(1L)).thenReturn(testFee);

        mockMvc.perform(get("/api/fees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(feeService, times(1)).getFee(1L);
    }

    @Test
    void deleteFee_ShouldReturnNoContent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        doNothing().when(feeService).deleteFee(1L);

        mockMvc.perform(delete("/api/fees/1"))
                .andExpect(status().isNoContent());

        verify(feeService, times(1)).deleteFee(1L);
    }

    @Test
    void getAllFees_ShouldReturnList() throws Exception {
        List<Fee> fees = Arrays.asList(testFee);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getAllFees(any(), any(), any())).thenReturn(fees);

        mockMvc.perform(get("/api/fees")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getAllFees(any(), any(), any());
    }

    @Test
    void getAllFeesPaginated_ShouldReturnPage() throws Exception {
        Page<Fee> feePage = new PageImpl<>(
                List.of(testFee),
                PageRequest.of(0, 10),
                1
        );

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getAllFeesPaginated(any(), any(), any(), any(PageRequest.class)))
                .thenReturn(feePage);

        mockMvc.perform(get("/api/fees/paginated")
                        .param("session", "2023/2024")
                        .param("term", "FIRST")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "dueDate")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(feeService, times(1))
                .getAllFeesPaginated(any(), any(), any(), any(PageRequest.class));
    }
    @Test
    void getStudentFees_ShouldReturnStudentFees() throws Exception {
        List<Fee> fees = Arrays.asList(testFee);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireFeeAccess(any(User.class), eq(1L));
        when(feeService.getStudentFees(eq(1L), anyString(), any(Fee.Term.class))).thenReturn(fees);

        mockMvc.perform(get("/api/fees/student/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getStudentFees(eq(1L), anyString(), any(Fee.Term.class));
    }

    @Test
    void recordPayment_ShouldReturnUpdatedFee() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.recordPayment(eq(1L), eq(25000.0), eq("CASH"), any(), any()))
                .thenReturn(testFee);

        mockMvc.perform(post("/api/fees/1/payment")
                        .param("amount", "25000")
                        .param("paymentMethod", "CASH")
                        .param("reference", "REF123")
                        .param("notes", "Partial payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(feeService, times(1)).recordPayment(eq(1L), eq(25000.0), eq("CASH"), any(), any());
    }

    @Test
    void recordPartialPayment_ShouldReturnUpdatedFee() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.recordPartialPayment(eq(1L), eq(10000.0), eq("CASH"), any()))
                .thenReturn(testFee);

        mockMvc.perform(post("/api/fees/1/partial-payment")
                        .param("amount", "10000")
                        .param("paymentMethod", "CASH")
                        .param("reference", "REF456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(feeService, times(1)).recordPartialPayment(eq(1L), eq(10000.0), eq("CASH"), any());
    }

    @Test
    void getFeeStatistics_ShouldReturnStatistics() throws Exception {
        FeeStatisticsDTO statistics = FeeStatisticsDTO.builder()
                .totalExpected(500000.0)
                .totalCollected(300000.0)
                .totalOutstanding(200000.0)
                .paidCount(10L)
                .pendingCount(3L)
                .partialCount(2L)
                .overdueCount(1L)
                .totalStudents(12L)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getFeeStatistics(anyString(), any(Fee.Term.class))).thenReturn(statistics);

        mockMvc.perform(get("/api/fees/statistics")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpected").value(500000.0))
                .andExpect(jsonPath("$.totalCollected").value(300000.0))
                .andExpect(jsonPath("$.totalOutstanding").value(200000.0));

        verify(feeService, times(1)).getFeeStatistics(anyString(), any(Fee.Term.class));
    }

    @Test
    void getDefaultingStudents_ShouldReturnList() throws Exception {
        DefaulterDTO defaulter = DefaulterDTO.builder()
                .studentId(1L)
                .studentName("John Doe")
                .outstandingBalance(50000.0)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getDefaultingStudents(anyString(), any(Fee.Term.class)))
                .thenReturn(List.of(defaulter));

        mockMvc.perform(get("/api/fees/defaulters")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getDefaultingStudents(anyString(), any(Fee.Term.class));
    }

    @Test
    void getOverdueFees_ShouldReturnList() throws Exception {
        List<Fee> overdueFees = Arrays.asList(testFee);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getOverdueFees()).thenReturn(overdueFees);

        mockMvc.perform(get("/api/fees/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getOverdueFees();
    }

    @Test
    void getUpcomingFees_ShouldReturnList() throws Exception {
        List<Fee> upcomingFees = Arrays.asList(testFee);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getUpcomingFees(7)).thenReturn(upcomingFees);

        mockMvc.perform(get("/api/fees/upcoming")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getUpcomingFees(7);
    }

    @Test
    void hasOutstandingFees_ShouldReturnBoolean() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireFeeAccess(any(User.class), eq(1L));
        when(feeService.hasOutstandingFees(eq(1L), anyString(), any(Fee.Term.class))).thenReturn(true);

        mockMvc.perform(get("/api/fees/student/1/has-outstanding")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOutstanding").value(true));

        verify(feeService, times(1)).hasOutstandingFees(eq(1L), anyString(), any(Fee.Term.class));
    }

    @Test
    void getTotalOutstanding_ShouldReturnAmount() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireFeeAccess(any(User.class), eq(1L));
        when(feeService.getTotalOutstanding(eq(1L), anyString(), any(Fee.Term.class))).thenReturn(50000.0);

        mockMvc.perform(get("/api/fees/student/1/outstanding/total")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutstanding").value(50000.0));

        verify(feeService, times(1)).getTotalOutstanding(eq(1L), anyString(), any(Fee.Term.class));
    }

    @Test
    void generateFeeReportPdf_ShouldReturnPdf() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.generateFeeReportPdf(anyString(), any(Fee.Term.class))).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/fees/report/pdf")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=fee-report.pdf"));

        verify(feeService, times(1)).generateFeeReportPdf(anyString(), any(Fee.Term.class));
    }

    @Test
    void generateReceipt_ShouldReturnPdf() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.generateReceipt(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/fees/1/receipt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=receipt-1.pdf"));

        verify(feeService, times(1)).generateReceipt(1L);
    }

    @Test
    void getFeesDueBetween_ShouldReturnList() throws Exception {
        List<Fee> fees = Arrays.asList(testFee);
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(feeService.getFeesDueBetween(eq(start), eq(end))).thenReturn(fees);

        mockMvc.perform(get("/api/fees/due-between")
                        .param("start", "2024-03-01")
                        .param("end", "2024-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getFeesDueBetween(eq(start), eq(end));
    }
}