package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.AnnouncementDTO;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.model.EmailLog;
import com.inkFront.schoolManagement.model.SmsLog;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.AnnouncementRepository;
import com.inkFront.schoolManagement.repository.EmailLogRepository;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.EmailNotificationService;
import com.inkFront.schoolManagement.service.EmailQueueService;
import com.inkFront.schoolManagement.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private SmsLogRepository smsLogRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private EmailQueueService emailQueueService;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private Announcement testAnnouncement;
    private AnnouncementDTO testDTO;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");
        testAnnouncement.setContent("Test Content");
        testAnnouncement.setType(Announcement.AnnouncementType.GENERAL);
        testAnnouncement.setPriority(Announcement.AnnouncementPriority.NORMAL);
        testAnnouncement.setAudience(Arrays.asList(Announcement.Audience.ALL));
        testAnnouncement.setActive(true);

        testDTO = AnnouncementDTO.builder()
                .title("Test Announcement")
                .content("Test Content")
                .type(Announcement.AnnouncementType.GENERAL)
                .priority(Announcement.AnnouncementPriority.NORMAL)
                .audience(Arrays.asList(Announcement.Audience.ALL))
                .active(true)
                .build();

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setParentPhone("08012345678");
        testStudent.setParentEmail("parent@example.com");
    }

    @Test
    void createAnnouncement_ShouldReturnCreatedAnnouncement() {
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        Announcement result = announcementService.createAnnouncement(testDTO);

        assertNotNull(result);
        assertEquals("Test Announcement", result.getTitle());
        verify(announcementRepository, times(1)).save(any(Announcement.class));
    }

    @Test
    void deleteAnnouncement_ShouldDeleteWithLogs() {
        List<SmsLog> smsLogs = Arrays.asList(new SmsLog(), new SmsLog());
        List<EmailLog> emailLogs = Arrays.asList(new EmailLog(), new EmailLog());

        when(smsLogRepository.findByAnnouncementId(1L)).thenReturn(smsLogs);
        when(emailLogRepository.findByAnnouncementId(1L)).thenReturn(emailLogs);
        doNothing().when(smsLogRepository).deleteAll(smsLogs);
        doNothing().when(emailLogRepository).deleteAll(emailLogs);
        doNothing().when(announcementRepository).deleteById(1L);

        announcementService.deleteAnnouncement(1L);

        verify(smsLogRepository, times(1)).findByAnnouncementId(1L);
        verify(emailLogRepository, times(1)).findByAnnouncementId(1L);
        verify(smsLogRepository, times(1)).deleteAll(smsLogs);
        verify(emailLogRepository, times(1)).deleteAll(emailLogs);
        verify(announcementRepository, times(1)).deleteById(1L);
    }

    @Test
    void getSchoolCalendar_ShouldReturnCalendar() {
        String session = "2023/2024";
        Map<String, Object> calendar = announcementService.getSchoolCalendar(session);

        assertNotNull(calendar);
        assertEquals(session, calendar.get("session"));
        assertNotNull(calendar.get("events"));
        assertTrue(((List<?>) calendar.get("events")).size() > 0);
    }
}