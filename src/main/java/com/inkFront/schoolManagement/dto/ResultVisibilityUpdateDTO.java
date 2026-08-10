package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultVisibilityUpdateDTO {

    private String visibilityMessage;

    @NotNull(message = "visibilityStatus is required")
    private ResultVisibilityStatus visibilityStatus;
}