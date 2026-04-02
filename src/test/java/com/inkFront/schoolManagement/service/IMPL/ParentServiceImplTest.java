package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentServiceImplTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ParentServiceImpl parentService;

    private Parent testParent;
    private ParentDTO testParentDTO;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testParent = new Parent();
        testParent.setId(1L);
        testParent.setFirstName("John");
        testParent.setLastName("Doe");
        testParent.setEmail("john.doe@example.com");
        testParent.setPhoneNumber("08012345678");
        testParent.setWards(new ArrayList<>());

        testParentDTO = new ParentDTO();
        testParentDTO.setId(1L);
        testParentDTO.setFirstName("John");
        testParentDTO.setLastName("Doe");
        testParentDTO.setEmail("john.doe@example.com");
        testParentDTO.setPhoneNumber("08012345678");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");
    }

    @Test
    void createParent_ShouldCreateParent() {
        when(parentRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(parentRepository.save(any(Parent.class))).thenReturn(testParent);

        ParentDTO result = parentService.createParent(testParentDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void createParent_WithExistingEmail_ShouldThrowException() {
        when(parentRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            parentService.createParent(testParentDTO);
        });

        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void updateParent_ShouldUpdateParent() {
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));
        when(parentRepository.save(any(Parent.class))).thenReturn(testParent);

        ParentDTO result = parentService.updateParent(1L, testParentDTO);

        assertNotNull(result);
        verify(parentRepository, times(1)).findById(1L);
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void updateParent_WithNonExistentId_ShouldThrowException() {
        when(parentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            parentService.updateParent(999L, testParentDTO);
        });

        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void deleteParent_ShouldDeleteParent() {
        testParent.setWards(new ArrayList<>());
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));
        doNothing().when(parentRepository).delete(testParent);

        parentService.deleteParent(1L);

        verify(parentRepository, times(1)).delete(testParent);
    }

    @Test
    void deleteParent_WithWards_ShouldThrowException() {
        testParent.setWards(Arrays.asList(testStudent));
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));

        assertThrows(RuntimeException.class, () -> {
            parentService.deleteParent(1L);
        });

        verify(parentRepository, never()).delete(any(Parent.class));
    }

    @Test
    void getParentById_ShouldReturnParent() {
        when(parentRepository.findByIdWithWards(1L)).thenReturn(Optional.of(testParent));

        Optional<ParentDTO> result = parentService.getParentById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void getAllParents_ShouldReturnList() {
        List<Parent> parents = Arrays.asList(testParent);
        when(parentRepository.findAll()).thenReturn(parents);

        List<ParentDTO> result = parentService.getAllParents();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllParentsPaginated_ShouldReturnPage() {
        Page<Parent> parentPage = new PageImpl<>(Arrays.asList(testParent));
        when(parentRepository.findAll(any(PageRequest.class))).thenReturn(parentPage);

        Page<ParentDTO> result = parentService.getAllParentsPaginated(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getParentByEmail_ShouldReturnParent() {
        when(parentRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(testParent));

        Optional<ParentDTO> result = parentService.getParentByEmail("john.doe@example.com");

        assertTrue(result.isPresent());
        assertEquals("john.doe@example.com", result.get().getEmail());
    }

    @Test
    void verifyParentEmail_ShouldReturnTrue() {
        when(parentRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        boolean result = parentService.verifyParentEmail("john.doe@example.com");

        assertTrue(result);
    }

    @Test
    void searchParents_ShouldReturnResults() {
        List<Parent> parents = Arrays.asList(testParent);
        when(parentRepository.searchParents("John")).thenReturn(parents);

        List<ParentDTO> result = parentService.searchParents("John");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void addWardToParent_ShouldAddWard() {
        // Setup
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setParent(testParent);
            return student;
        });

        // Create a parent WITH the ward for the DTO
        Parent parentWithWard = new Parent();
        parentWithWard.setId(1L);
        parentWithWard.setFirstName("John");
        parentWithWard.setLastName("Doe");
        parentWithWard.setEmail("john.doe@example.com");
        parentWithWard.setPhoneNumber("08012345678");
        List<Student> wards = new ArrayList<>();
        wards.add(testStudent);
        parentWithWard.setWards(wards);

        when(parentRepository.findByIdWithWards(1L)).thenReturn(Optional.of(parentWithWard));

        ParentDTO result = parentService.addWardToParent(1L, 1L);

        assertNotNull(result);
        assertEquals(testParent, testStudent.getParent());
        assertNotNull(result.getWards());
        assertEquals(1, result.getWards().size());
        assertEquals(testStudent.getId(), result.getWards().get(0).getId());
        verify(studentRepository, times(1)).save(testStudent);
    }

    @Test
    void addWardToParent_WithNonExistentParent_ShouldThrowException() {
        when(parentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            parentService.addWardToParent(999L, 1L);
        });

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void addWardToParent_WithNonExistentStudent_ShouldThrowException() {
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            parentService.addWardToParent(1L, 999L);
        });

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void removeWardFromParent_ShouldRemoveWard() {
        testStudent.setParent(testParent);
        when(parentRepository.findById(1L)).thenReturn(Optional.of(testParent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // Create a parent without the ward for the DTO
        Parent parentWithoutWard = new Parent();
        parentWithoutWard.setId(1L);
        parentWithoutWard.setFirstName("John");
        parentWithoutWard.setLastName("Doe");
        parentWithoutWard.setEmail("john.doe@example.com");
        parentWithoutWard.setPhoneNumber("08012345678");
        parentWithoutWard.setWards(new ArrayList<>());

        when(parentRepository.findByIdWithWards(1L)).thenReturn(Optional.of(parentWithoutWard));

        ParentDTO result = parentService.removeWardFromParent(1L, 1L);

        assertNotNull(result);
        assertNull(testStudent.getParent());
        assertEquals(0, result.getWards().size());
        verify(studentRepository, times(1)).save(testStudent);
    }

    @Test
    void getParentsWithNoWards_ShouldReturnList() {
        List<Parent> parents = Arrays.asList(testParent);
        when(parentRepository.findParentsWithNoWards()).thenReturn(parents);

        List<ParentDTO> result = parentService.getParentsWithNoWards();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void createMultipleParents_ShouldCreateAll() {
        List<ParentDTO> parents = Arrays.asList(testParentDTO, testParentDTO);
        when(parentRepository.saveAll(anyList())).thenReturn(Arrays.asList(testParent, testParent));

        List<ParentDTO> result = parentService.createMultipleParents(parents);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(parentRepository, times(1)).saveAll(anyList());
    }

    @Test
    void getTotalParentCount_ShouldReturnCount() {
        when(parentRepository.count()).thenReturn(10L);

        long result = parentService.getTotalParentCount();

        assertEquals(10L, result);
    }
}