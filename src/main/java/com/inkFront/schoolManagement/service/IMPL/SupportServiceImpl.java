package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.support.CreateSupportTicketRequest;
import com.inkFront.schoolManagement.dto.support.SendSupportMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportMessageDTO;
import com.inkFront.schoolManagement.dto.support.SupportTicketDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.SupportMessage;
import com.inkFront.schoolManagement.model.SupportTicket;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.SupportMessageRepository;
import com.inkFront.schoolManagement.repository.SupportTicketRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportServiceImpl implements SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication.getName();

        return userRepository.findByUsernameOrEmail(principal)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    private String generateTicketNumber() {
        String year = String.valueOf(Year.now().getValue());
        long sequence = supportTicketRepository.count() + 1;

        String ticketNumber = "SUP-" + year + "-" + String.format("%05d", sequence);
        while (supportTicketRepository.existsByTicketNumber(ticketNumber)) {
            sequence++;
            ticketNumber = "SUP-" + year + "-" + String.format("%05d", sequence);
        }

        return ticketNumber;
    }

    private SupportTicket getAccessibleTicket(Long ticketId, User currentUser) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (isAdmin(currentUser)) {
            return ticket;
        }

        if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to access this ticket");
        }

        return ticket;
    }

    private SupportTicketDTO toTicketDTO(SupportTicket ticket) {
        List<SupportMessageDTO> messages = supportMessageRepository.findByTicketOrderByCreatedAtAsc(ticket)
                .stream()
                .map(SupportMessageDTO::fromEntity)
                .collect(Collectors.toList());

        return SupportTicketDTO.fromEntity(ticket, messages);
    }

    @Override
    public SupportTicketDTO createTicket(CreateSupportTicketRequest request) {
        User currentUser = getCurrentUser();

        if (isAdmin(currentUser)) {
            throw new BusinessException("Admin should reply to support tickets, not create requester tickets");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setSubject(request.getSubject().trim());
        ticket.setCategory(request.getCategory() != null && !request.getCategory().trim().isEmpty()
                ? request.getCategory().trim()
                : null);
        ticket.setStatus(SupportTicket.TicketStatus.OPEN);
        ticket.setCreatedBy(currentUser);
        ticket.setAssignedAdmin(null);
        ticket.setRequesterUnread(false);
        ticket.setAdminUnread(true);
        ticket.setLastMessageAt(LocalDateTime.now());

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        SupportMessage firstMessage = new SupportMessage();
        firstMessage.setTicket(savedTicket);
        firstMessage.setSender(currentUser);
        firstMessage.setFromAdmin(false);
        firstMessage.setMessage(request.getMessage().trim());
        supportMessageRepository.save(firstMessage);

        return toTicketDTO(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getMyTickets() {
        User currentUser = getCurrentUser();

        return supportTicketRepository.findByCreatedByOrderByUpdatedAtDesc(currentUser)
                .stream()
                .map(SupportTicketDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTicketsForAdmin() {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new AccessDeniedException("Only admin can view all support tickets");
        }

        return supportTicketRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(SupportTicketDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SupportTicketDTO getTicketDetails(Long ticketId) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = getAccessibleTicket(ticketId, currentUser);

        if (isAdmin(currentUser)) {
            ticket.setAdminUnread(false);
            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
        } else {
            ticket.setRequesterUnread(false);
        }

        SupportTicket saved = supportTicketRepository.save(ticket);
        return toTicketDTO(saved);
    }

    @Override
    public SupportTicketDTO sendMessage(Long ticketId, SendSupportMessageRequest request) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = getAccessibleTicket(ticketId, currentUser);

        boolean fromAdmin = isAdmin(currentUser);

        if (!fromAdmin && ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new BusinessException("This ticket is closed. Reopen it before sending a message.");
        }

        SupportMessage message = new SupportMessage();
        message.setTicket(ticket);
        message.setSender(currentUser);
        message.setFromAdmin(fromAdmin);
        message.setMessage(request.getMessage().trim());
        supportMessageRepository.save(message);

        ticket.setLastMessageAt(LocalDateTime.now());

        if (fromAdmin) {
            ticket.setStatus(SupportTicket.TicketStatus.ANSWERED);
            ticket.setRequesterUnread(true);
            ticket.setAdminUnread(false);

            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
        } else {
            if (ticket.getStatus() == SupportTicket.TicketStatus.ANSWERED) {
                ticket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
            } else if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
                ticket.setStatus(SupportTicket.TicketStatus.OPEN);
            } else {
                ticket.setStatus(SupportTicket.TicketStatus.OPEN);
            }

            ticket.setAdminUnread(true);
            ticket.setRequesterUnread(false);
        }

        SupportTicket saved = supportTicketRepository.save(ticket);
        return toTicketDTO(saved);
    }

    @Override
    public SupportTicketDTO closeTicket(Long ticketId) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = getAccessibleTicket(ticketId, currentUser);

        ticket.setStatus(SupportTicket.TicketStatus.CLOSED);

        if (isAdmin(currentUser)) {
            ticket.setRequesterUnread(true);
            ticket.setAdminUnread(false);
            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
        } else {
            ticket.setAdminUnread(true);
            ticket.setRequesterUnread(false);
        }

        return toTicketDTO(supportTicketRepository.save(ticket));
    }

    @Override
    public SupportTicketDTO reopenTicket(Long ticketId) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = getAccessibleTicket(ticketId, currentUser);

        ticket.setStatus(SupportTicket.TicketStatus.OPEN);

        if (isAdmin(currentUser)) {
            ticket.setRequesterUnread(true);
            ticket.setAdminUnread(false);
            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
        } else {
            ticket.setAdminUnread(true);
            ticket.setRequesterUnread(false);
        }

        return toTicketDTO(supportTicketRepository.save(ticket));
    }
}