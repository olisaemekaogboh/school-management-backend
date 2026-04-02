package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.AnnouncementDTO;
import com.inkFront.schoolManagement.dto.EmailLogDTO;
import com.inkFront.schoolManagement.dto.SmsLogDTO;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.repository.EmailLogRepository;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.service.AnnouncementService;
import com.inkFront.schoolManagement.service.NotificationService;
import com.inkFront.schoolManagement.service.SmsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private SmsLogRepository smsLogRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AnnouncementController announcementController;

    private ObjectMapper objectMapper;
    private Announcement testAnnouncement;
    private AnnouncementDTO testAnnouncementDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(announcementController).build();
        objectMapper = new ObjectMapper();

        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");
        testAnnouncement.setContent("Test Content");
        testAnnouncement.setType(Announcement.AnnouncementType.GENERAL);
        testAnnouncement.setPriority(Announcement.AnnouncementPriority.NORMAL);
        testAnnouncement.setAudience(Arrays.asList(Announcement.Audience.ALL));
        testAnnouncement.setActive(true);

        testAnnouncementDTO = AnnouncementDTO.fromAnnouncement(testAnnouncement);
    }

    @Test
    void createAnnouncement_ShouldReturnCreatedAnnouncement() throws Exception {
        when(announcementService.createAnnouncement(any(AnnouncementDTO.class))).thenReturn(testAnnouncement);

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAnnouncementDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Announcement"));

        verify(announcementService, times(1)).createAnnouncement(any(AnnouncementDTO.class));
    }

    @Test
    void sendNotifications_ShouldReturnSmsResult() throws Exception {
        SmsResult smsResult = SmsResult.builder()
                .successCount(10)
                .failedCount(0)
                .message("Notifications sent successfully")
                .build();

        when(notificationService.sendAnnouncementNotifications(1L)).thenReturn(smsResult);

        mockMvc.perform(post("/api/announcements/1/notify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(10));

        verify(notificationService, times(1)).sendAnnouncementNotifications(1L);
    }

    @Test
    void updateAnnouncement_ShouldReturnUpdatedAnnouncement() throws Exception {
        when(announcementService.updateAnnouncement(eq(1L), any(AnnouncementDTO.class))).thenReturn(testAnnouncement);

        mockMvc.perform(put("/api/announcements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAnnouncementDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(announcementService, times(1)).updateAnnouncement(eq(1L), any(AnnouncementDTO.class));
    }

    @Test
    void deleteAnnouncement_ShouldReturnNoContent() throws Exception {
        doNothing().when(announcementService).deleteAnnouncement(1L);

        mockMvc.perform(delete("/api/announcements/1"))
                .andExpect(status().isNoContent());

        verify(announcementService, times(1)).deleteAnnouncement(1L);
    }

    @Test
    void getAnnouncement_ShouldReturnAnnouncement() throws Exception {
        when(announcementService.getAnnouncement(1L)).thenReturn(testAnnouncement);

        mockMvc.perform(get("/api/announcements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(announcementService, times(1)).getAnnouncement(1L);
    }

    @Test
    void getAllActiveAnnouncements_ShouldReturnList() throws Exception {
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        when(announcementService.getAllActiveAnnouncements()).thenReturn(announcements);

        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(announcementService, times(1)).getAllActiveAnnouncements();
    }
}