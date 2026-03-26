package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.support.CreateSupportTicketRequest;
import com.inkFront.schoolManagement.dto.support.SendSupportMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportTicketDTO;
import com.inkFront.schoolManagement.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyRole('PARENT','TEACHER','STUDENT')")
    public ResponseEntity<SupportTicketDTO> createTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        return new ResponseEntity<>(supportService.createTicket(request), HttpStatus.CREATED);
    }

    @GetMapping("/tickets/my")
    @PreAuthorize("hasAnyRole('PARENT','TEACHER','STUDENT')")
    public ResponseEntity<List<SupportTicketDTO>> getMyTickets() {
        return ResponseEntity.ok(supportService.getMyTickets());
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicketDTO>> getAllTicketsForAdmin() {
        return ResponseEntity.ok(supportService.getAllTicketsForAdmin());
    }

    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT','TEACHER','STUDENT')")
    public ResponseEntity<SupportTicketDTO> getTicketDetails(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.getTicketDetails(ticketId));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT','TEACHER','STUDENT')")
    public ResponseEntity<SupportTicketDTO> sendMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody SendSupportMessageRequest request
    ) {
        return ResponseEntity.ok(supportService.sendMessage(ticketId, request));
    }

    @PatchMapping("/tickets/{ticketId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT','TEACHER','STUDENT')")
    public ResponseEntity<SupportTicketDTO> closeTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.closeTicket(ticketId));
    }

    @PatchMapping("/tickets/{ticketId}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT','TEACHER','STUDENT')")
    public ResponseEntity<SupportTicketDTO> reopenTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.reopenTicket(ticketId));
    }
}