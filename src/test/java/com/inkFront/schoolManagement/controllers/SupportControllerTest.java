package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.support.CreateSupportTicketRequest;
import com.inkFront.schoolManagement.dto.support.SendSupportMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportTicketDTO;
import com.inkFront.schoolManagement.service.SupportService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportService supportService;

    @InjectMocks
    private SupportController supportController;

    private ObjectMapper objectMapper;
    private CreateSupportTicketRequest createRequest;
    private SupportTicketDTO ticketDTO;
    private SendSupportMessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supportController).build();
        objectMapper = new ObjectMapper();

        createRequest = new CreateSupportTicketRequest();
        createRequest.setSubject("Login Issue");
        createRequest.setMessage("Unable to login to the system");
        createRequest.setCategory("Technical");

        ticketDTO = new SupportTicketDTO();
        ticketDTO.setId(1L);
        ticketDTO.setSubject("Login Issue");
        ticketDTO.setCategory("Technical");
        ticketDTO.setStatus("OPEN");
        ticketDTO.setCreatedAt(LocalDateTime.now());

        // 🔥 THIS WAS MISSING
        messageRequest = new SendSupportMessageRequest();
        messageRequest.setMessage("This is a support reply");
    }

    @Test
    void createTicket_ShouldReturnCreatedTicket() throws Exception {
        when(supportService.createTicket(any(CreateSupportTicketRequest.class))).thenReturn(ticketDTO);

        mockMvc.perform(post("/api/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.subject").value("Login Issue"));

        verify(supportService, times(1)).createTicket(any(CreateSupportTicketRequest.class));
    }

    @Test
    void getMyTickets_ShouldReturnList() throws Exception {
        List<SupportTicketDTO> tickets = Arrays.asList(ticketDTO);
        when(supportService.getMyTickets()).thenReturn(tickets);

        mockMvc.perform(get("/api/support/tickets/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(supportService, times(1)).getMyTickets();
    }

    @Test
    void getAllTicketsForAdmin_ShouldReturnList() throws Exception {
        List<SupportTicketDTO> tickets = Arrays.asList(ticketDTO);
        when(supportService.getAllTicketsForAdmin()).thenReturn(tickets);

        mockMvc.perform(get("/api/support/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(supportService, times(1)).getAllTicketsForAdmin();
    }

    @Test
    void getTicketDetails_ShouldReturnTicket() throws Exception {
        when(supportService.getTicketDetails(1L)).thenReturn(ticketDTO);

        mockMvc.perform(get("/api/support/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(supportService, times(1)).getTicketDetails(1L);
    }

    @Test
    void sendMessage_ShouldReturnUpdatedTicket() throws Exception {
        when(supportService.sendMessage(eq(1L), any(SendSupportMessageRequest.class))).thenReturn(ticketDTO);

        mockMvc.perform(post("/api/support/tickets/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(supportService, times(1)).sendMessage(eq(1L), any(SendSupportMessageRequest.class));
    }

    @Test
    void closeTicket_ShouldReturnClosedTicket() throws Exception {
        ticketDTO.setStatus("CLOSED");
        when(supportService.closeTicket(1L)).thenReturn(ticketDTO);

        mockMvc.perform(patch("/api/support/tickets/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(supportService, times(1)).closeTicket(1L);
    }

    @Test
    void reopenTicket_ShouldReturnReopenedTicket() throws Exception {
        ticketDTO.setStatus("REOPENED");
        when(supportService.reopenTicket(1L)).thenReturn(ticketDTO);

        mockMvc.perform(patch("/api/support/tickets/1/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));

        verify(supportService, times(1)).reopenTicket(1L);
    }

    @Test
    void createTicket_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreateSupportTicketRequest invalidRequest = new CreateSupportTicketRequest();
        // Missing required fields

        mockMvc.perform(post("/api/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(supportService, never()).createTicket(any());
    }
}
