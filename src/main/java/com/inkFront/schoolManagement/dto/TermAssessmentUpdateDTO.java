package com.inkFront.schoolManagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermAssessmentUpdateDTO {

    @Valid
    @Builder.Default
    private List<AssessmentItemDTO> characterTraits = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<AssessmentItemDTO> psychomotorTraits = new ArrayList<>();

    @Size(max = 1000, message = "Class teacher comment cannot exceed 1000 characters")
    private String classTeacherComment;

    @Size(max = 1000, message = "Principal comment cannot exceed 1000 characters")
    private String principalComment;

    private LocalDate nextTermBegins;
}