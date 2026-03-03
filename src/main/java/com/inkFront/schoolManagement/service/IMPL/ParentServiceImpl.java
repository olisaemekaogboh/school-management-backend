// src/main/java/com/inkFront/schoolManagement/service/IMPL/ParentServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.ParentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    @Override
    public ParentDTO createParent(ParentDTO parentDTO) {
        log.info("Creating new parent with email: {}", parentDTO.getEmail());

        // Check if email already exists
        if (parentRepository.existsByEmail(parentDTO.getEmail())) {
            throw new RuntimeException("Parent with email " + parentDTO.getEmail() + " already exists");
        }

        Parent parent = ParentDTO.toEntity(parentDTO);
        Parent savedParent = parentRepository.save(parent);
        log.info("Parent created successfully with ID: {}", savedParent.getId());

        return ParentDTO.fromEntity(savedParent);
    }

    @Override
    public ParentDTO updateParent(Long id, ParentDTO parentDTO) {
        log.info("Updating parent with ID: {}", id);

        Parent existingParent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + id));

        // Check if email is being changed and if it's already taken
        if (!existingParent.getEmail().equals(parentDTO.getEmail()) &&
                parentRepository.existsByEmail(parentDTO.getEmail())) {
            throw new RuntimeException("Email " + parentDTO.getEmail() + " is already taken");
        }

        // Update fields
        existingParent.setFirstName(parentDTO.getFirstName());
        existingParent.setLastName(parentDTO.getLastName());
        existingParent.setMiddleName(parentDTO.getMiddleName());
        existingParent.setEmail(parentDTO.getEmail());
        existingParent.setPhoneNumber(parentDTO.getPhoneNumber());
        existingParent.setAlternatePhone(parentDTO.getAlternatePhone());
        existingParent.setAddress(parentDTO.getAddress());
        existingParent.setOccupation(parentDTO.getOccupation());
        existingParent.setCompanyName(parentDTO.getCompanyName());
        existingParent.setOfficeAddress(parentDTO.getOfficeAddress());
        existingParent.setRelationship(parentDTO.getRelationship() != null ?
                Parent.Relationship.valueOf(parentDTO.getRelationship()) : null);
        existingParent.setEmergencyContactName(parentDTO.getEmergencyContactName());
        existingParent.setEmergencyContactPhone(parentDTO.getEmergencyContactPhone());
        existingParent.setEmergencyContactRelationship(parentDTO.getEmergencyContactRelationship());
        existingParent.setProfilePictureUrl(parentDTO.getProfilePictureUrl());

        Parent updatedParent = parentRepository.save(existingParent);
        log.info("Parent updated successfully with ID: {}", updatedParent.getId());

        return ParentDTO.fromEntity(updatedParent);
    }

    @Override
    public void deleteParent(Long id) {
        log.info("Deleting parent with ID: {}", id);

        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + id));

        // Check if parent has wards
        if (parent.getWards() != null && !parent.getWards().isEmpty()) {
            throw new RuntimeException("Cannot delete parent with assigned wards. Please reassign wards first.");
        }

        parentRepository.delete(parent);
        log.info("Parent deleted successfully with ID: {}", id);
    }

    @Override
    public Optional<ParentDTO> getParentById(Long id) {
        log.info("Fetching parent with ID: {}", id);
        return parentRepository.findByIdWithWards(id)
                .map(ParentDTO::fromEntity);
    }

    @Override
    public List<ParentDTO> getAllParents() {
        log.info("Fetching all parents");
        return parentRepository.findAll().stream()
                .map(ParentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ParentDTO> getAllParentsPaginated(Pageable pageable) {
        log.info("Fetching parents page: {}", pageable.getPageNumber());
        return parentRepository.findAll(pageable)
                .map(ParentDTO::fromEntity);
    }

    @Override
    public Optional<ParentDTO> getParentByEmail(String email) {
        log.info("Fetching parent with email: {}", email);
        return parentRepository.findByEmailIgnoreCase(email)
                .map(ParentDTO::fromEntity);
    }

    @Override
    public boolean verifyParentEmail(String email) {
        log.info("Verifying parent email: {}", email);
        boolean exists = parentRepository.existsByEmail(email);
        log.info("Email {} exists: {}", email, exists);
        return exists;
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return parentRepository.existsByEmail(email);
    }

    @Override
    public List<ParentDTO> searchParents(String searchTerm) {
        log.info("Searching parents with term: {}", searchTerm);
        return parentRepository.searchParents(searchTerm).stream()
                .map(ParentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParentDTO addWardToParent(Long parentId, Long studentId) {
        log.info("Adding ward {} to parent {}", studentId, parentId);

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + parentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        student.setParent(parent);
        studentRepository.save(student);

        return ParentDTO.fromEntity(parentRepository.findByIdWithWards(parentId).orElse(parent));
    }

    @Override
    @Transactional
    public ParentDTO removeWardFromParent(Long parentId, Long studentId) {
        log.info("Removing ward {} from parent {}", studentId, parentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        student.setParent(null);
        studentRepository.save(student);

        return getParentById(parentId).orElse(null);
    }

    @Override
    public List<ParentDTO> getParentsWithNoWards() {
        log.info("Fetching parents with no wards");
        return parentRepository.findParentsWithNoWards().stream()
                .map(ParentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParentDTO> createMultipleParents(List<ParentDTO> parentDTOs) {
        log.info("Creating {} parents", parentDTOs.size());

        List<Parent> parents = parentDTOs.stream()
                .map(ParentDTO::toEntity)
                .collect(Collectors.toList());

        List<Parent> savedParents = parentRepository.saveAll(parents);

        return savedParents.stream()
                .map(ParentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long getTotalParentCount() {
        return parentRepository.count();
    }
}