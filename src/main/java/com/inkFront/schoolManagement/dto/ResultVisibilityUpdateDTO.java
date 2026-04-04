package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultVisibilityUpdateDTO {

    @NotBlank(message = "visibilityStatus is required")


    private String visibilityMessage;
    private ResultVisibilityStatus visibilityStatus;
}