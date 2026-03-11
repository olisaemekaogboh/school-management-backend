package com.inkFront.schoolManagement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.inkFront.schoolManagement.model.Term;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SessionRequestDTO {

    @NotBlank(message = "Session name is required")
    @JsonAlias({"session", "sessionName"})
    private String sessionName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Term currentTerm = Term.FIRST;

    private boolean active = false;
}