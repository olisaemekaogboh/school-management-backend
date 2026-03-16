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

        if (parentDTO.getEmail() != null && parentRepository.existsByEmail(parentDTO.getEmail())) {
            throw new RuntimeException("Parent with email " + parentDTO.getEmail() + " already exists");
        }

        Parent parent = mapToEntity(parentDTO);
        Parent savedParent = parentRepository.save(parent);

        log.info("Parent created successfully with ID: {}", savedParent.getId());
        return ParentDTO.fromParent(savedParent);
    }

    @Override
    public ParentDTO updateParent(Long id, ParentDTO parentDTO) {
        log.info("Updating parent with ID: {}", id);

        Parent existingParent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + id));

        if (parentDTO.getEmail() != null
                && existingParent.getEmail() != null
                && !existingParent.getEmail().equalsIgnoreCase(parentDTO.getEmail())
                && parentRepository.existsByEmail(parentDTO.getEmail())) {
            throw new RuntimeException("Email " + parentDTO.getEmail() + " is already taken");
        }

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
        existingParent.setRelationship(parentDTO.getRelationship());
        existingParent.setEmergencyContactName(parentDTO.getEmergencyContactName());
        existingParent.setEmergencyContactPhone(parentDTO.getEmergencyContactPhone());
        existingParent.setEmergencyContactRelationship(parentDTO.getEmergencyContactRelationship());
        existingParent.setProfilePictureUrl(parentDTO.getProfilePictureUrl());

        Parent updatedParent = parentRepository.save(existingParent);
        log.info("Parent updated successfully with ID: {}", updatedParent.getId());

        return ParentDTO.fromParent(updatedParent);
    }

    @Override
    public void deleteParent(Long id) {
        log.info("Deleting parent with ID: {}", id);

        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + id));

        if (parent.getWards() != null && !parent.getWards().isEmpty()) {
            throw new RuntimeException("Cannot delete parent with assigned wards. Please reassign wards first.");
        }

        parentRepository.delete(parent);
        log.info("Parent deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentDTO> getParentById(Long id) {
        log.info("Fetching parent with ID: {}", id);
        return parentRepository.findByIdWithWards(id)
                .map(ParentDTO::fromParent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentDTO> getAllParents() {
        log.info("Fetching all parents");
        return parentRepository.findAll().stream()
                .map(ParentDTO::fromParent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParentDTO> getAllParentsPaginated(Pageable pageable) {
        log.info("Fetching parents page: {}", pageable.getPageNumber());
        return parentRepository.findAll(pageable)
                .map(ParentDTO::fromParent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentDTO> getParentByEmail(String email) {
        log.info("Fetching parent with email: {}", email);
        return parentRepository.findByEmailIgnoreCase(email)
                .map(ParentDTO::fromParent);
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
    @Transactional(readOnly = true)
    public List<ParentDTO> searchParents(String searchTerm) {
        log.info("Searching parents with term: {}", searchTerm);
        return parentRepository.searchParents(searchTerm).stream()
                .map(ParentDTO::fromParent)
                .collect(Collectors.toList());
    }

    @Override
    public ParentDTO addWardToParent(Long parentId, Long studentId) {
        log.info("Adding ward {} to parent {}", studentId, parentId);

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + parentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        student.setParent(parent);
        studentRepository.save(student);

        Parent refreshedParent = parentRepository.findByIdWithWards(parentId).orElse(parent);
        return ParentDTO.fromParent(refreshedParent);
    }

    @Override
    public ParentDTO removeWardFromParent(Long parentId, Long studentId) {
        log.info("Removing ward {} from parent {}", studentId, parentId);

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent not found with ID: " + parentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        if (student.getParent() == null || !student.getParent().getId().equals(parentId)) {
            throw new RuntimeException("Student is not linked to this parent");
        }

        student.setParent(null);
        studentRepository.save(student);

        Parent refreshedParent = parentRepository.findByIdWithWards(parentId).orElse(parent);
        return ParentDTO.fromParent(refreshedParent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentDTO> getParentsWithNoWards() {
        log.info("Fetching parents with no wards");
        return parentRepository.findParentsWithNoWards().stream()
                .map(ParentDTO::fromParent)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParentDTO> createMultipleParents(List<ParentDTO> parentDTOs) {
        log.info("Creating {} parents", parentDTOs.size());

        List<Parent> parents = parentDTOs.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());

        List<Parent> savedParents = parentRepository.saveAll(parents);

        return savedParents.stream()
                .map(ParentDTO::fromParent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalParentCount() {
        return parentRepository.count();
    }

    private Parent mapToEntity(ParentDTO dto) {
        Parent parent = new Parent();

        parent.setFirstName(dto.getFirstName());
        parent.setLastName(dto.getLastName());
        parent.setMiddleName(dto.getMiddleName());
        parent.setEmail(dto.getEmail());
        parent.setPhoneNumber(dto.getPhoneNumber());
        parent.setAlternatePhone(dto.getAlternatePhone());
        parent.setAddress(dto.getAddress());
        parent.setOccupation(dto.getOccupation());
        parent.setCompanyName(dto.getCompanyName());
        parent.setOfficeAddress(dto.getOfficeAddress());
        parent.setRelationship(dto.getRelationship());
        parent.setEmergencyContactName(dto.getEmergencyContactName());
        parent.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        parent.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        parent.setProfilePictureUrl(dto.getProfilePictureUrl());

        return parent;
    }
}