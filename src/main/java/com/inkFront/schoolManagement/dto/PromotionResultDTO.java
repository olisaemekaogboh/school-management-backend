// src/main/java/com/inkFront/schoolManagement/dto/PromotionResultDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResultDTO {

    private int promoted;
    private int retained;
    private int graduated;
    private int total;
    private String session;
    private List<Map<String, String>> details;
}