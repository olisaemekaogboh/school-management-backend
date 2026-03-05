// src/main/java/com/inkFront/schoolManagement/service/AcademicSessionService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.AcademicSessionRequest;
import com.inkFront.schoolManagement.dto.AcademicSessionResponse;

import java.util.List;

public interface AcademicSessionService {
    AcademicSessionResponse create(AcademicSessionRequest request);
    AcademicSessionResponse update(Long id, AcademicSessionRequest request);
    AcademicSessionResponse activate(Long id);
    AcademicSessionResponse getActive();
    List<AcademicSessionResponse> getAll();
    void delete(Long id);
}