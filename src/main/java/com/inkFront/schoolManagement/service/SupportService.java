package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.support.CreateSupportTicketRequest;
import com.inkFront.schoolManagement.dto.support.SendSupportMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportTicketDTO;

import java.util.List;

public interface SupportService {
    SupportTicketDTO createTicket(CreateSupportTicketRequest request);
    List<SupportTicketDTO> getMyTickets();
    List<SupportTicketDTO> getAllTicketsForAdmin();
    SupportTicketDTO getTicketDetails(Long ticketId);
    SupportTicketDTO sendMessage(Long ticketId, SendSupportMessageRequest request);
    SupportTicketDTO closeTicket(Long ticketId);
    SupportTicketDTO reopenTicket(Long ticketId);
}