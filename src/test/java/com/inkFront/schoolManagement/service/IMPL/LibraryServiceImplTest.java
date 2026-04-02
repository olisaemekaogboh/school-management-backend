package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.BookBorrowingDTO;
import com.inkFront.schoolManagement.dto.BookDTO;
import com.inkFront.schoolManagement.dto.BorrowRequestDTO;
import com.inkFront.schoolManagement.model.Book;
import com.inkFront.schoolManagement.model.BookBorrowing;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.repository.BookBorrowingRepository;
import com.inkFront.schoolManagement.repository.BookRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookBorrowingRepository borrowingRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private LibraryServiceImpl libraryService;

    private Book testBook;
    private BookDTO testBookDTO;
    private Student testStudent;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("The Great Gatsby");
        testBook.setAuthor("F. Scott Fitzgerald");
        testBook.setIsbn("978-0-7432-7356-5");
        testBook.setTotalCopies(5);
        testBook.setAvailableCopies(5);
        testBook.setStatus(Book.BookStatus.AVAILABLE);

        testBookDTO = new BookDTO();
        testBookDTO.setId(1L);
        testBookDTO.setTitle("The Great Gatsby");
        testBookDTO.setAuthor("F. Scott Fitzgerald");
        testBookDTO.setIsbn("978-0-7432-7356-5");
        testBookDTO.setTotalCopies(5);
        testBookDTO.setAvailableCopies(5);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("Jane");
        testTeacher.setLastName("Smith");
        testTeacher.setEmployeeId("TCH001");
        testTeacher.setTeacherId("TCH001");
    }

    @Test
    void createBook_ShouldCreateBook() {
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookDTO result = libraryService.createBook(testBookDTO);

        assertNotNull(result);
        assertEquals("The Great Gatsby", result.getTitle());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void createBook_WithInvalidCopies_ShouldThrowException() {
        testBookDTO.setTotalCopies(-1);

        assertThrows(RuntimeException.class, () -> {
            libraryService.createBook(testBookDTO);
        });
    }

    @Test
    void updateBook_ShouldUpdateBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookDTO result = libraryService.updateBook(1L, testBookDTO);

        assertNotNull(result);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void updateBook_WithNonExistentId_ShouldThrowException() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            libraryService.updateBook(999L, testBookDTO);
        });
    }

    @Test
    void getBook_ShouldReturnBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        BookDTO result = libraryService.getBook(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void deleteBook_ShouldDeleteBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(borrowingRepository.findAll()).thenReturn(new ArrayList<>());
        doNothing().when(bookRepository).delete(testBook);

        libraryService.deleteBook(1L);

        verify(bookRepository, times(1)).delete(testBook);
    }

    @Test
    void deleteBook_WithActiveBorrowings_ShouldThrowException() {
        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setBook(testBook);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(borrowingRepository.findAll()).thenReturn(Arrays.asList(borrowing));

        assertThrows(RuntimeException.class, () -> {
            libraryService.deleteBook(1L);
        });
    }

    @Test
    void getAllBooks_ShouldReturnList() {
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findAll()).thenReturn(books);

        List<BookDTO> result = libraryService.getAllBooks();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchBooks_ShouldReturnResults() {
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
                anyString(), anyString(), anyString()))
                .thenReturn(books);

        List<BookDTO> result = libraryService.searchBooks("Gatsby");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBooksByCategory_ShouldReturnFilteredList() {
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByCategoryIgnoreCase("Fiction")).thenReturn(books);

        List<BookDTO> result = libraryService.getBooksByCategory("Fiction");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void borrowBook_ByStudent_ShouldCreateBorrowing() {
        BorrowRequestDTO request = new BorrowRequestDTO();
        request.setBookId(1L);
        request.setStudentId(1L);
        request.setDueDate(LocalDate.now().plusDays(14).toString());

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(borrowingRepository.save(any(BookBorrowing.class))).thenReturn(new BookBorrowing());
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookBorrowingDTO result = libraryService.borrowBook(request);

        assertNotNull(result);
        verify(borrowingRepository, times(1)).save(any(BookBorrowing.class));
        verify(bookRepository, times(1)).save(any(Book.class));
        assertEquals(4, testBook.getAvailableCopies());
    }

    @Test
    void borrowBook_ByTeacher_ShouldCreateBorrowing() {
        BorrowRequestDTO request = new BorrowRequestDTO();
        request.setBookId(1L);
        request.setTeacherId(1L);
        request.setDueDate(LocalDate.now().plusDays(14).toString());

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(borrowingRepository.save(any(BookBorrowing.class))).thenReturn(new BookBorrowing());
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookBorrowingDTO result = libraryService.borrowBook(request);

        assertNotNull(result);
        verify(borrowingRepository, times(1)).save(any(BookBorrowing.class));
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void borrowBook_WithNoAvailableCopies_ShouldThrowException() {
        testBook.setAvailableCopies(0);
        BorrowRequestDTO request = new BorrowRequestDTO();
        request.setBookId(1L);
        request.setStudentId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        assertThrows(RuntimeException.class, () -> {
            libraryService.borrowBook(request);
        });

        verify(borrowingRepository, never()).save(any(BookBorrowing.class));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void borrowBook_WithNoBorrower_ShouldThrowException() {
        BorrowRequestDTO request = new BorrowRequestDTO();
        request.setBookId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        assertThrows(RuntimeException.class, () -> {
            libraryService.borrowBook(request);
        });

        verify(borrowingRepository, never()).save(any(BookBorrowing.class));
        verify(bookRepository, never()).save(any(Book.class));
    }
    @Test
    void returnBook_ShouldUpdateBorrowing() {
        Book book = new Book();
        book.setId(1L);
        book.setAvailableCopies(5);
        book.setTotalCopies(5);
        book.setStatus(Book.BookStatus.AVAILABLE);

        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setId(1L);
        borrowing.setBook(book);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);
        borrowing.setBorrowDate(LocalDate.now().minusDays(5));
        borrowing.setDueDate(LocalDate.now().plusDays(9));

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));
        when(borrowingRepository.save(any(BookBorrowing.class))).thenReturn(borrowing);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookBorrowingDTO result = libraryService.returnBook(1L);

        assertNotNull(result);
        assertEquals(BookBorrowing.BorrowingStatus.RETURNED, borrowing.getStatus());
        assertNotNull(borrowing.getReturnDate());

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository, times(1)).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();

        assertEquals(5, savedBook.getAvailableCopies());
    }
    @Test
    void returnBook_WithAlreadyReturned_ShouldThrowException() {
        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setId(1L);
        borrowing.setBook(testBook);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.RETURNED);

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));

        assertThrows(RuntimeException.class, () -> {
            libraryService.returnBook(1L);
        });

        verify(borrowingRepository, never()).save(any(BookBorrowing.class));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void renewBook_ShouldExtendDueDate() {
        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setId(1L);
        borrowing.setBook(testBook);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);
        borrowing.setDueDate(LocalDate.now());

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));
        when(borrowingRepository.save(any(BookBorrowing.class))).thenReturn(borrowing);

        BookBorrowingDTO result = libraryService.renewBook(1L);

        assertNotNull(result);
        assertTrue(borrowing.getDueDate().isAfter(LocalDate.now()));
        verify(borrowingRepository, times(1)).save(borrowing);
    }

    @Test
    void renewBook_WithAlreadyReturned_ShouldThrowException() {
        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setId(1L);
        borrowing.setBook(testBook);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.RETURNED);

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));

        assertThrows(RuntimeException.class, () -> {
            libraryService.renewBook(1L);
        });

        verify(borrowingRepository, never()).save(any(BookBorrowing.class));
    }

    @Test
    void reportLost_ShouldMarkBookAsLost() {
        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setId(1L);
        borrowing.setBook(testBook);
        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));
        when(borrowingRepository.save(any(BookBorrowing.class))).thenReturn(borrowing);

        BookBorrowingDTO result = libraryService.reportLost(1L);

        assertNotNull(result);
        assertEquals(BookBorrowing.BorrowingStatus.LOST, borrowing.getStatus());
        verify(borrowingRepository, times(1)).save(borrowing);
    }

    @Test
    void getAllBorrowings_ShouldReturnList() {
        List<BookBorrowing> borrowings = Arrays.asList(new BookBorrowing());
        when(borrowingRepository.findAll()).thenReturn(borrowings);

        List<BookBorrowingDTO> result = libraryService.getAllBorrowings();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBorrowingsByStudent_ShouldReturnList() {
        List<BookBorrowing> borrowings = Arrays.asList(new BookBorrowing());
        when(borrowingRepository.findByStudent_Id(1L)).thenReturn(borrowings);

        List<BookBorrowingDTO> result = libraryService.getBorrowingsByStudent(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBorrowingsByTeacher_ShouldReturnList() {
        List<BookBorrowing> borrowings = Arrays.asList(new BookBorrowing());
        when(borrowingRepository.findByTeacher_Id(1L)).thenReturn(borrowings);

        List<BookBorrowingDTO> result = libraryService.getBorrowingsByTeacher(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getOverdueBorrowings_ShouldReturnList() {
        BookBorrowing overdue = new BookBorrowing();
        overdue.setId(1L);
        overdue.setStatus(BookBorrowing.BorrowingStatus.BORROWED);
        overdue.setDueDate(LocalDate.now().minusDays(1));

        when(borrowingRepository.findByStatusAndDueDateBefore(
                eq(BookBorrowing.BorrowingStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(Arrays.asList(overdue));
        when(borrowingRepository.saveAll(anyList())).thenReturn(Arrays.asList(overdue));
        when(borrowingRepository.findByStatus(BookBorrowing.BorrowingStatus.OVERDUE))
                .thenReturn(Arrays.asList(overdue));

        List<BookBorrowingDTO> result = libraryService.getOverdueBorrowings();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getLibraryStatistics_ShouldReturnStats() {
        List<Book> books = Arrays.asList(testBook);
        List<BookBorrowing> borrowings = Arrays.asList(new BookBorrowing());

        when(bookRepository.findAll()).thenReturn(books);
        when(borrowingRepository.findAll()).thenReturn(borrowings);

        Map<String, Object> stats = libraryService.getLibraryStatistics();

        assertNotNull(stats);
        assertEquals(1L, stats.get("totalBooks"));
        assertEquals(5, stats.get("totalCopies"));
        assertEquals(5, stats.get("availableCopies"));
    }
}