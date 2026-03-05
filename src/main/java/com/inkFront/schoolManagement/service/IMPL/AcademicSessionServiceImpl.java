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
import java.util.Optional;

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

        String name = request.getName().trim();

        // ✅ better: ignore case duplicates
        if (repo.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Session already exists: " + name);
        }

        boolean makeActive = Boolean.TRUE.equals(request.getActive());

        AcademicSession s = new AcademicSession();
        s.setName(name);
        s.setStartYear(request.getStartYear());
        s.setEndYear(request.getEndYear());
        s.setActive(makeActive);

        AcademicSession saved = repo.save(s);

        if (makeActive) {
            repo.deactivateOthers(saved.getId());
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
            if (!newName.equalsIgnoreCase(s.getName()) && repo.existsByNameIgnoreCase(newName)) {
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
            repo.deactivateOthers(saved.getId());
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
        repo.deactivateOthers(saved.getId());

        return map(saved);
    }

    // ✅ SAFE: returns Optional
    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicSessionResponse> getActiveOptional() {
        // Try active first
        Optional<AcademicSession> active = repo.findFirstByActiveTrue();
        if (active.isPresent()) return active.map(this::map);

        // If none active, you can either return empty (recommended),
        // OR fall back to "latest by startYear".
        // I’ll keep your fallback behavior but still return Optional:

        return repo.findAll().stream()
                .max(Comparator.comparing(AcademicSession::getStartYear))
                .map(this::map);
    }

    // ✅ SAFE: returns null if none
    @Override
    @Transactional(readOnly = true)
    public AcademicSessionResponse getActiveOrNull() {
        return getActiveOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
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
}