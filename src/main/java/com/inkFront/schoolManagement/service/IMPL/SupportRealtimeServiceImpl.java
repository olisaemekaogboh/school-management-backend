package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.support.SupportWsMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportWsMessageResponse;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.SupportMessage;
import com.inkFront.schoolManagement.model.SupportTicket;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.SupportMessageRepository;
import com.inkFront.schoolManagement.repository.SupportTicketRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.SupportRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportRealtimeServiceImpl implements SupportRealtimeService {

    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public SupportWsMessageResponse handleRealtimeMessage(String principalName, SupportWsMessageRequest request) {
        User currentUser = userRepository.findByUsernameOrEmail(principalName)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        SupportTicket ticket = supportTicketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + request.getTicketId()));

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        boolean isOwner = ticket.getCreatedBy() != null
                && ticket.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to send messages to this ticket");
        }

        if (!isAdmin && ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new BusinessException("This ticket is closed. Reopen it before sending a message.");
        }

        SupportMessage message = new SupportMessage();
        message.setTicket(ticket);
        message.setSender(currentUser);
        message.setFromAdmin(isAdmin);
        message.setMessage(request.getMessage().trim());
        SupportMessage savedMessage = supportMessageRepository.save(message);

        ticket.setLastMessageAt(LocalDateTime.now());

        if (isAdmin) {
            ticket.setStatus(SupportTicket.TicketStatus.ANSWERED);
            ticket.setRequesterUnread(true);
            ticket.setAdminUnread(false);

            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
        } else {
            ticket.setStatus(SupportTicket.TicketStatus.OPEN);
            ticket.setAdminUnread(true);
            ticket.setRequesterUnread(false);
        }

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        String senderName = ((currentUser.getFirstName() != null ? currentUser.getFirstName() : "") + " "
                + (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim();

        SupportWsMessageResponse payload = SupportWsMessageResponse.builder()
                .ticketId(savedTicket.getId())
                .messageId(savedMessage.getId())
                .senderName(senderName)
                .senderRole(currentUser.getRole().name())
                .fromAdmin(isAdmin)
                .message(savedMessage.getMessage())
                .ticketStatus(savedTicket.getStatus().name())
                .createdAt(savedMessage.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/support/" + savedTicket.getId(), payload);

        if (ticket.getCreatedBy() != null) {
            messagingTemplate.convertAndSendToUser(
                    ticket.getCreatedBy().getEmail(),
                    "/queue/support",
                    payload
            );
            messagingTemplate.convertAndSendToUser(
                    ticket.getCreatedBy().getUsername(),
                    "/queue/support",
                    payload
            );
        }

        if (ticket.getAssignedAdmin() != null) {
            messagingTemplate.convertAndSendToUser(
                    ticket.getAssignedAdmin().getEmail(),
                    "/queue/support",
                    payload
            );
            messagingTemplate.convertAndSendToUser(
                    ticket.getAssignedAdmin().getUsername(),
                    "/queue/support",
                    payload
            );
        }

        return payload;
    }
}