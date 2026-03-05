package com.inkFront.schoolManagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class SchoolClassDTO {

    private Long id;

    private String className;

    private String classCode;

    private String category;

    private String description;

    private Long classTeacherId;

    private Integer capacity;

    private Integer currentEnrollment;

    private List<String> subjects;

}