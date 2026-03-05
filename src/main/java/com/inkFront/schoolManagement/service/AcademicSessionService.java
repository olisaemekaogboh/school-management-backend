// src/main/java/com/inkFront/schoolManagement/service/AcademicSessionService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.AcademicSessionRequest;
import com.inkFront.schoolManagement.dto.AcademicSessionResponse;

import java.util.List;
import java.util.Optional;

public interface AcademicSessionService {
    AcademicSessionResponse create(AcademicSessionRequest request);
    AcademicSessionResponse update(Long id, AcademicSessionRequest request);
    AcademicSessionResponse activate(Long id);

    // old (if you want to keep it): AcademicSessionResponse getActive();

    // ✅ safe for UI
    Optional<AcademicSessionResponse> getActiveOptional();
    AcademicSessionResponse getActiveOrNull();

    List<AcademicSessionResponse> getAll();
    void delete(Long id);
}