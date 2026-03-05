
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.SchoolClassDTO;

import java.util.List;

public interface SchoolClassService {

    SchoolClassDTO createClass(SchoolClassDTO dto);

    SchoolClassDTO updateClass(Long id, SchoolClassDTO dto);

    SchoolClassDTO getClass(Long id);

    List<SchoolClassDTO> getAllClasses();

    void deleteClass(Long id);

}