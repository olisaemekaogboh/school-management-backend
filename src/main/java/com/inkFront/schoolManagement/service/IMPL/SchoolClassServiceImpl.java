package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SchoolClassDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.service.SchoolClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class SchoolClassServiceImpl implements SchoolClassService {

    private final ClassRepository classRepository;

    private SchoolClassDTO toDTO(SchoolClass c) {

        SchoolClassDTO dto = new SchoolClassDTO();

        dto.setId(c.getId());
        dto.setClassName(c.getClassName());
        dto.setClassCode(c.getClassCode());
        dto.setDescription(c.getDescription());
        dto.setCapacity(c.getCapacity());
        dto.setCurrentEnrollment(c.getCurrentEnrollment());
        dto.setSubjects(c.getSubjects());

        if (c.getCategory() != null)
            dto.setCategory(c.getCategory().name());

        if (c.getClassTeacher() != null)
            dto.setClassTeacherId(c.getClassTeacher().getId());

        return dto;
    }

    private void apply(SchoolClass c, SchoolClassDTO dto) {

        c.setClassName(dto.getClassName());
        c.setClassCode(dto.getClassCode());
        c.setDescription(dto.getDescription());
        c.setCapacity(dto.getCapacity());
        c.setCurrentEnrollment(dto.getCurrentEnrollment());
        c.setSubjects(dto.getSubjects());

        if (dto.getCategory() != null) {
            c.setCategory(SchoolClass.ClassCategory.valueOf(dto.getCategory()));
        }
    }

    @Override
    public SchoolClassDTO createClass(SchoolClassDTO dto) {

        SchoolClass c = new SchoolClass();

        apply(c, dto);

        return toDTO(classRepository.save(c));
    }

    @Override
    public SchoolClassDTO updateClass(Long id, SchoolClassDTO dto) {

        SchoolClass c = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        apply(c, dto);

        return toDTO(classRepository.save(c));
    }

    @Override
    public SchoolClassDTO getClass(Long id) {

        return classRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Class not found"));
    }

    @Override
    public List<SchoolClassDTO> getAllClasses() {

        return classRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    public void deleteClass(Long id) {

        classRepository.deleteById(id);
    }
}