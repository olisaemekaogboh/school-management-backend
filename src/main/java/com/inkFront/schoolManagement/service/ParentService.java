// src/main/java/com/inkFront/schoolManagement/service/ParentService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.ParentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ParentService {

    // Basic CRUD operations
    ParentDTO createParent(ParentDTO parentDTO);
    ParentDTO updateParent(Long id, ParentDTO parentDTO);
    void deleteParent(Long id);
    Optional<ParentDTO> getParentById(Long id);
    List<ParentDTO> getAllParents();
    Page<ParentDTO> getAllParentsPaginated(Pageable pageable);

    // Email related operations
    Optional<ParentDTO> getParentByEmail(String email);
    boolean verifyParentEmail(String email);
    boolean isEmailRegistered(String email);

    // Search operations
    List<ParentDTO> searchParents(String searchTerm);

    // Ward related operations
    ParentDTO addWardToParent(Long parentId, Long studentId);
    ParentDTO removeWardFromParent(Long parentId, Long studentId);
    List<ParentDTO> getParentsWithNoWards();

    // Bulk operations
    List<ParentDTO> createMultipleParents(List<ParentDTO> parentDTOs);

    // Statistics
    long getTotalParentCount();
}