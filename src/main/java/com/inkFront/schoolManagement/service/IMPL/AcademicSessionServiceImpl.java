// src/main/java/com/inkFront/schoolManagement/service/IMPL/AcademicSessionServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.AcademicSessionRequest;
import com.inkFront.schoolManagement.dto.AcademicSessionResponse;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.AcademicSession;
import com.inkFront.schoolManagement.repository.AcademicSessionRepository;
import com.inkFront.schoolManagement.service.AcademicSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AcademicSessionServiceImpl implements AcademicSessionService {

    private final AcademicSessionRepository repo;

    public AcademicSessionServiceImpl(AcademicSessionRepository repo) {
        this.repo = repo;
    }

    private AcademicSessionResponse map(AcademicSession s) {
        return new AcademicSessionResponse(
                s.getId(),
                s.getName(),
                s.getStartYear(),
                s.getEndYear(),
                s.getActive(),
                s.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public AcademicSessionResponse create(AcademicSessionRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Session name is required (e.g. 2025/2026).");
        }
        if (request.getStartYear() == null || request.getEndYear() == null) {
            throw new IllegalArgumentException("startYear and endYear are required.");
        }
        if (repo.existsByName(request.getName().trim())) {
            throw new IllegalArgumentException("Session already exists: " + request.getName());
        }

        AcademicSession s = new AcademicSession();
        s.setName(request.getName().trim());
        s.setStartYear(request.getStartYear());
        s.setEndYear(request.getEndYear());
        s.setActive(Boolean.TRUE.equals(request.getActive()));

        AcademicSession saved = repo.save(s);

        // If created active => deactivate others
        if (Boolean.TRUE.equals(saved.getActive())) {
            deactivateOthers(saved.getId());
        }

        return map(saved);
    }

    @Override
    @Transactional
    public AcademicSessionResponse update(Long id, AcademicSessionRequest request) {
        AcademicSession s = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSession not found: " + id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(s.getName()) && repo.existsByName(newName)) {
                throw new IllegalArgumentException("Session name already exists: " + newName);
            }
            s.setName(newName);
        }
        if (request.getStartYear() != null) s.setStartYear(request.getStartYear());
        if (request.getEndYear() != null) s.setEndYear(request.getEndYear());

        if (request.getActive() != null) {
            s.setActive(request.getActive());
        }

        AcademicSession saved = repo.save(s);

        if (Boolean.TRUE.equals(saved.getActive())) {
            deactivateOthers(saved.getId());
        }

        return map(saved);
    }

    @Override
    @Transactional
    public AcademicSessionResponse activate(Long id) {
        AcademicSession s = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSession not found: " + id));

        s.setActive(true);
        AcademicSession saved = repo.save(s);
        deactivateOthers(saved.getId());
        return map(saved);
    }

    @Override
    public AcademicSessionResponse getActive() {
        AcademicSession active = repo.findByActiveTrue()
                .orElseGet(() -> repo.findAll().stream()
                        .max(Comparator.comparing(AcademicSession::getStartYear))
                        .orElseThrow(() -> new ResourceNotFoundException("No sessions found.")));
        return map(active);
    }

    @Override
    public List<AcademicSessionResponse> getAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartYear).reversed())
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AcademicSession s = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSession not found: " + id));
        repo.delete(s);
    }

    private void deactivateOthers(Long activeId) {
        List<AcademicSession> all = repo.findAll();
        for (AcademicSession other : all) {
            if (!other.getId().equals(activeId) && Boolean.TRUE.equals(other.getActive())) {
                other.setActive(false);
                repo.save(other);
            }
        }
    }
}