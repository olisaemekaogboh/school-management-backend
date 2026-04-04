package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import lombok.Data;

@Data
public class ResultVisibilityUpdateDTO {

    private ResultVisibilityStatus visibilityStatus;

    private String visibilityMessage;
}