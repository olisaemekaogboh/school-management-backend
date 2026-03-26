package com.inkFront.schoolManagement.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSupportTicketRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 150)
    private String subject;

    @Size(max = 50)
    private String category;

    @NotBlank(message = "Message is required")
    @Size(max = 5000)
    private String message;
}