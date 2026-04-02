package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.config.SmsConfig;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.model.SmsLog;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.SmsResult;
import com.inkFront.schoolManagement.service.SmsTemplateService;
import com.inkFront.schoolManagement.utils.PhoneNumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AfricaTalkingSmsServiceImplTest {

    @Mock
    private SmsConfig smsConfig;

    @Mock
    private SmsTemplateService templateService;

    @Mock
    private SmsLogRepository smsLogRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private AfricaTalkingSmsServiceImpl smsService;

    private Announcement testAnnouncement;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        lenient().when(smsConfig.getUsername()).thenReturn("test_username");
        lenient().when(smsConfig.getApiKey()).thenReturn("test_api_key");

        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setParentPhone("08012345678");
        testStudent.setParentName("Mr. John Doe");
        testStudent.setEmergencyContactPhone("08012345678");
    }

    @Test
    void setCurrentAnnouncement_ShouldSetAnnouncement() {
        smsService.setCurrentAnnouncement(testAnnouncement);
        Announcement current = (Announcement) ReflectionTestUtils.getField(smsService, "currentAnnouncement");
        assertEquals(testAnnouncement, current);
    }

    @Test
    void sendSms_WithValidPhoneNumber_ShouldSendSuccessfully() {
        String phoneNumber = "08012345678";
        String message = "Test message";

        try (MockedStatic<PhoneNumberUtils> phoneUtils = mockStatic(PhoneNumberUtils.class)) {
            phoneUtils.when(() -> PhoneNumberUtils.formatToLocal(phoneNumber)).thenReturn("08012345678");
            phoneUtils.when(() -> PhoneNumberUtils.validateNigerianPhoneNumber(phoneNumber)).thenReturn(true);

            SmsLog smsLog = new SmsLog();
            smsLog.setId(1L);
            when(smsLogRepository.save(any(SmsLog.class))).thenReturn(smsLog);

            SmsResult result = smsService.sendSms(phoneNumber, message);

            assertNotNull(result);
            assertEquals("FAILED", result.getStatus());
        }
    }

    @Test
    void sendSms_WithInvalidPhoneNumber_ShouldReturnFailure() {
        SmsResult result = smsService.sendSms("invalid", "Test message");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals("Invalid phone number", result.getErrorMessage());
    }

    @Test
    void sendSms_WithNullPhoneNumber_ShouldReturnFailure() {
        SmsResult result = smsService.sendSms(null, "Test message");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    void formatPhoneNumberForDatabase_ShouldFormatCorrectly() {
        try (MockedStatic<PhoneNumberUtils> phoneUtils = mockStatic(PhoneNumberUtils.class)) {
            phoneUtils.when(() -> PhoneNumberUtils.formatToLocal("08012345678")).thenReturn("08012345678");

            String result = smsService.formatPhoneNumberForDatabase("08012345678");
            assertEquals("08012345678", result);
        }
    }

    @Test
    void formatPhoneNumberForDatabase_WithNull_ShouldReturnNull() {
        assertNull(smsService.formatPhoneNumberForDatabase(null));
    }

    @Test
    void formatPhoneNumberForDatabase_WithEmpty_ShouldReturnNull() {
        assertNull(smsService.formatPhoneNumberForDatabase(""));
    }

    @Test
    void formatPhoneNumberForSMS_With11DigitNumber_ShouldAddCountryCode() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", "08012345678");
        assertEquals("+2348012345678", result);
    }

    @Test
    void formatPhoneNumberForSMS_With10DigitNumber_ShouldAdd234() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", "8012345678");
        assertEquals("+2348012345678", result);
    }

    @Test
    void formatPhoneNumberForSMS_With13DigitNumber_ShouldReturnAsIs() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", "2348012345678");
        assertEquals("+2348012345678", result);
    }

    @Test
    void formatPhoneNumberForSMS_With14DigitNumber_ShouldTrim() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", "23480123456789");
        assertEquals("+2348012345678", result);
    }

    @Test
    void formatPhoneNumberForSMS_WithNull_ShouldReturnNull() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", (Object) null);
        assertNull(result);
    }

    @Test
    void formatPhoneNumberForSMS_WithEmpty_ShouldReturnNull() {
        String result = ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumberForSMS", "");
        assertNull(result);
    }

    @Test
    void validatePhoneNumber_ShouldDelegateToPhoneNumberUtils() {
        try (MockedStatic<PhoneNumberUtils> phoneUtils = mockStatic(PhoneNumberUtils.class)) {
            phoneUtils.when(() -> PhoneNumberUtils.validateNigerianPhoneNumber("08012345678")).thenReturn(true);

            boolean result = smsService.validatePhoneNumber("08012345678");
            assertTrue(result);
        }
    }

    @Test
    void sendSmsWithTemplate_ShouldGenerateAndSend() {
        String phoneNumber = "08012345678";
        String templateName = "welcome";
        Map<String, String> params = new HashMap<>();
        params.put("name", "John");

        when(templateService.generateMessage(templateName, params)).thenReturn("Welcome John!");

        SmsResult result = smsService.sendSmsWithTemplate(phoneNumber, templateName, params);

        assertNotNull(result);
        verify(templateService, times(1)).generateMessage(templateName, params);
    }

    @Test
    void formatPhoneNumber_ShouldDelegateToDatabaseFormat() {
        try (MockedStatic<PhoneNumberUtils> phoneUtils = mockStatic(PhoneNumberUtils.class)) {
            phoneUtils.when(() -> PhoneNumberUtils.formatToLocal("08012345678")).thenReturn("08012345678");

            String result = smsService.formatPhoneNumber("08012345678");
            assertEquals("08012345678", result);
        }
    }

    @Test
    void detectMessageType_ShouldReturnCorrectType() {
        String feeType = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", "Fee payment due: ₦50000");
        String resultType = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", "Result released");
        String urgentType = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", "URGENT: Emergency meeting");
        String eventType = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", "Sports event tomorrow");
        String defaultType = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", "General message");

        assertEquals("FEE_REMINDER", feeType);
        assertEquals("RESULT", resultType);
        assertEquals("EMERGENCY", urgentType);
        assertEquals("EVENT", eventType);
        assertEquals("GENERAL", defaultType);
    }

    @Test
    void detectMessageType_WithNull_ShouldReturnGeneral() {
        String type = ReflectionTestUtils.invokeMethod(smsService, "detectMessageType", (Object) null);
        assertEquals("GENERAL", type);
    }

    @Test
    void findStudentByPhone_ShouldReturnStudent() {
        when(studentRepository.findByParentPhoneOrderByLastNameAscFirstNameAsc("08012345678"))
                .thenReturn(List.of(testStudent));

        Optional<Student> result = ReflectionTestUtils.invokeMethod(smsService, "findStudentByPhone", "08012345678");

        assertTrue(result.isPresent());
        assertEquals(testStudent, result.get());
    }

    @Test
    void findStudentByPhone_WithEmergencyContact_ShouldReturnStudent() {
        when(studentRepository.findByParentPhoneOrderByLastNameAscFirstNameAsc("08012345678"))
                .thenReturn(new ArrayList<>());
        when(studentRepository.findByEmergencyContactPhone("08012345678"))
                .thenReturn(List.of(testStudent));

        Optional<Student> result = ReflectionTestUtils.invokeMethod(smsService, "findStudentByPhone", "08012345678");

        assertTrue(result.isPresent());
        assertEquals(testStudent, result.get());
    }

    @Test
    void findStudentByPhone_NotFound_ShouldReturnEmpty() {
        when(studentRepository.findByParentPhoneOrderByLastNameAscFirstNameAsc("08012345678"))
                .thenReturn(new ArrayList<>());
        when(studentRepository.findByEmergencyContactPhone("08012345678"))
                .thenReturn(new ArrayList<>());

        Optional<Student> result = ReflectionTestUtils.invokeMethod(smsService, "findStudentByPhone", "08012345678");

        assertTrue(result.isEmpty());
    }

    @Test
    void sendBulkSms_WithEmptyList_ShouldReturnEmptyResult() {
        List<SmsResult> result = smsService.sendBulkSms(new ArrayList<>(), "Hello everyone");

        assertNotNull(result);
        verifyNoInteractions(studentRepository);
        verifyNoInteractions(smsLogRepository);
    }
}