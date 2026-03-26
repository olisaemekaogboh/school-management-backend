package com.inkFront.schoolManagement.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendSupportMessageRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 5000)
    private String message;
}