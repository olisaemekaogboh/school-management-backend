package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.BookBorrowingDTO;
import com.inkFront.schoolManagement.dto.BookDTO;
import com.inkFront.schoolManagement.dto.BorrowRequestDTO;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import com.inkFront.schoolManagement.service.LibraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LibraryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LibraryService libraryService;

    @InjectMocks
    private LibraryController libraryController;

    private ObjectMapper objectMapper;
    private BookDTO testBookDTO;
    private BookBorrowingDTO testBorrowingDTO;
    private BorrowRequestDTO testBorrowRequestDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(libraryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testBookDTO = new BookDTO();
        testBookDTO.setId(1L);
        testBookDTO.setTitle("The Great Gatsby");
        testBookDTO.setAuthor("F. Scott Fitzgerald");
        testBookDTO.setIsbn("978-0-7432-7356-5");
        testBookDTO.setPublisher("Scribner");
        testBookDTO.setPublicationDate("1925-04-10");
        testBookDTO.setCategory("Fiction");
        testBookDTO.setTotalCopies(5);
        testBookDTO.setAvailableCopies(3);

        testBorrowRequestDTO = new BorrowRequestDTO();
        testBorrowRequestDTO.setBookId(1L);
        testBorrowRequestDTO.setStudentId(1L);
        testBorrowRequestDTO.setDueDate(LocalDate.now().plusDays(14).toString());

        testBorrowingDTO = new BookBorrowingDTO();
        testBorrowingDTO.setId(1L);
        testBorrowingDTO.setBookId(1L);
        testBorrowingDTO.setBookTitle("The Great Gatsby");
        testBorrowingDTO.setStatus("BORROWED");
        testBorrowingDTO.setDueDate(LocalDate.now().plusDays(14).toString());
    }

    @Test
    void createBook_ShouldReturnCreatedBook() throws Exception {
        when(libraryService.createBook(any(BookDTO.class))).thenReturn(testBookDTO);

        mockMvc.perform(post("/api/library/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("The Great Gatsby"));

        verify(libraryService, times(1)).createBook(any(BookDTO.class));
    }

    @Test
    void updateBook_ShouldReturnUpdatedBook() throws Exception {
        when(libraryService.updateBook(eq(1L), any(BookDTO.class))).thenReturn(testBookDTO);

        mockMvc.perform(put("/api/library/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(libraryService, times(1)).updateBook(eq(1L), any(BookDTO.class));
    }

    @Test
    void getBook_ShouldReturnBook() throws Exception {
        when(libraryService.getBook(1L)).thenReturn(testBookDTO);

        mockMvc.perform(get("/api/library/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("The Great Gatsby"));

        verify(libraryService, times(1)).getBook(1L);
    }

    @Test
    void deleteBook_ShouldReturnNoContent() throws Exception {
        doNothing().when(libraryService).deleteBook(1L);

        mockMvc.perform(delete("/api/library/books/1"))
                .andExpect(status().isOk());

        verify(libraryService, times(1)).deleteBook(1L);
    }

    @Test
    void getAllBooks_ShouldReturnList() throws Exception {
        List<BookDTO> books = Arrays.asList(testBookDTO);
        when(libraryService.getAllBooks()).thenReturn(books);

        mockMvc.perform(get("/api/library/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getAllBooks();
    }

    @Test
    void searchBooks_ShouldReturnSearchResults() throws Exception {
        List<BookDTO> books = Arrays.asList(testBookDTO);
        when(libraryService.searchBooks("Gatsby")).thenReturn(books);

        mockMvc.perform(get("/api/library/books/search")
                        .param("term", "Gatsby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).searchBooks("Gatsby");
    }

    @Test
    void getBooksByCategory_ShouldReturnFilteredList() throws Exception {
        List<BookDTO> books = Arrays.asList(testBookDTO);
        when(libraryService.getBooksByCategory("Fiction")).thenReturn(books);

        mockMvc.perform(get("/api/library/books/category/Fiction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getBooksByCategory("Fiction");
    }

    @Test
    void borrowBook_ShouldCreateBorrowing() throws Exception {
        when(libraryService.borrowBook(any(BorrowRequestDTO.class))).thenReturn(testBorrowingDTO);

        mockMvc.perform(post("/api/library/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBorrowRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.bookTitle").value("The Great Gatsby"));

        verify(libraryService, times(1)).borrowBook(any(BorrowRequestDTO.class));
    }

    @Test
    void returnBook_ShouldUpdateBorrowing() throws Exception {
        testBorrowingDTO.setStatus("RETURNED");
        when(libraryService.returnBook(1L)).thenReturn(testBorrowingDTO);

        mockMvc.perform(post("/api/library/return/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        verify(libraryService, times(1)).returnBook(1L);
    }

    @Test
    void renewBook_ShouldExtendDueDate() throws Exception {
        testBorrowingDTO.setDueDate(LocalDate.now().plusDays(28).toString());
        when(libraryService.renewBook(1L)).thenReturn(testBorrowingDTO);

        mockMvc.perform(post("/api/library/renew/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").exists());

        verify(libraryService, times(1)).renewBook(1L);
    }

    @Test
    void reportLost_ShouldUpdateBorrowing() throws Exception {
        testBorrowingDTO.setStatus("LOST");
        when(libraryService.reportLost(1L)).thenReturn(testBorrowingDTO);

        mockMvc.perform(post("/api/library/lost/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"));

        verify(libraryService, times(1)).reportLost(1L);
    }

    @Test
    void getAllBorrowings_ShouldReturnList() throws Exception {
        List<BookBorrowingDTO> borrowings = Arrays.asList(testBorrowingDTO);
        when(libraryService.getAllBorrowings()).thenReturn(borrowings);

        mockMvc.perform(get("/api/library/borrowings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getAllBorrowings();
    }

    @Test
    void getBorrowingsByStudent_ShouldReturnStudentBorrowings() throws Exception {
        List<BookBorrowingDTO> borrowings = Arrays.asList(testBorrowingDTO);
        when(libraryService.getBorrowingsByStudent(1L)).thenReturn(borrowings);

        mockMvc.perform(get("/api/library/borrowings/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getBorrowingsByStudent(1L);
    }

    @Test
    void getBorrowingsByTeacher_ShouldReturnTeacherBorrowings() throws Exception {
        List<BookBorrowingDTO> borrowings = Arrays.asList(testBorrowingDTO);
        when(libraryService.getBorrowingsByTeacher(1L)).thenReturn(borrowings);

        mockMvc.perform(get("/api/library/borrowings/teacher/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getBorrowingsByTeacher(1L);
    }

    @Test
    void getOverdueBorrowings_ShouldReturnList() throws Exception {
        List<BookBorrowingDTO> overdue = Arrays.asList(testBorrowingDTO);
        when(libraryService.getOverdueBorrowings()).thenReturn(overdue);

        mockMvc.perform(get("/api/library/borrowings/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(libraryService, times(1)).getOverdueBorrowings();
    }

    @Test
    void getLibraryStatistics_ShouldReturnStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", 100);
        stats.put("totalBorrowings", 50);
        stats.put("availableBooks", 60);
        stats.put("overdueBooks", 5);

        when(libraryService.getLibraryStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/library/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBooks").value(100))
                .andExpect(jsonPath("$.totalBorrowings").value(50));

        verify(libraryService, times(1)).getLibraryStatistics();
    }

    @Test
    void borrowBook_WithUnavailableBook_ShouldReturnBadRequest() throws Exception {
        when(libraryService.borrowBook(any(BorrowRequestDTO.class)))
                .thenThrow(new RuntimeException("Book not available"));

        mockMvc.perform(post("/api/library/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBorrowRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(libraryService, times(1)).borrowBook(any(BorrowRequestDTO.class));
    }
}
