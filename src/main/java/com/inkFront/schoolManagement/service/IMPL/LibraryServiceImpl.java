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
import com.inkFront.schoolManagement.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional
public class LibraryServiceImpl implements LibraryService {

    private final BookRepository bookRepository;
    private final BookBorrowingRepository borrowingRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    private BookDTO toDTO(Book b) {
        BookDTO dto = new BookDTO();
        dto.setId(b.getId());
        dto.setTitle(b.getTitle());
        dto.setAuthor(b.getAuthor());
        dto.setIsbn(b.getIsbn());
        dto.setCategory(b.getCategory());
        dto.setTotalCopies(b.getTotalCopies());
        dto.setAvailableCopies(b.getAvailableCopies());
        return dto;
    }

    private BookBorrowingDTO toDTO(BookBorrowing bb) {
        BookBorrowingDTO dto = new BookBorrowingDTO();
        dto.setId(bb.getId());

        if (bb.getBook() != null) {
            dto.setBookId(bb.getBook().getId());
            dto.setBookTitle(bb.getBook().getTitle());
        }

        // IMPORTANT: your entity likely has Student/Teacher objects (ManyToOne),
        // so map safely (avoid NullPointerException)
        dto.setStudentId(bb.getStudent() != null ? bb.getStudent().getId() : null);
        dto.setTeacherId(bb.getTeacher() != null ? bb.getTeacher().getId() : null);

        dto.setBorrowDate(bb.getBorrowDate() != null ? bb.getBorrowDate().toString() : null);
        dto.setDueDate(bb.getDueDate() != null ? bb.getDueDate().toString() : null);
        dto.setReturnDate(bb.getReturnDate() != null ? bb.getReturnDate().toString() : null);

        // status is enum -> convert to String
        dto.setStatus(bb.getStatus() != null ? bb.getStatus().name() : null);

        return dto;
    }

    @Override
    public BookDTO createBook(BookDTO dto) {
        Book b = new Book();
        b.setTitle(dto.getTitle());
        b.setAuthor(dto.getAuthor());
        b.setIsbn(dto.getIsbn());
        b.setCategory(dto.getCategory());
        b.setTotalCopies(dto.getTotalCopies() == null ? 0 : dto.getTotalCopies());
        b.setAvailableCopies(dto.getAvailableCopies() == null ? b.getTotalCopies() : dto.getAvailableCopies());
        return toDTO(bookRepository.save(b));
    }

    @Override
    public BookDTO updateBook(Long id, BookDTO dto) {
        Book b = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        b.setTitle(dto.getTitle());
        b.setAuthor(dto.getAuthor());
        b.setIsbn(dto.getIsbn());
        b.setCategory(dto.getCategory());
        if (dto.getTotalCopies() != null) b.setTotalCopies(dto.getTotalCopies());
        if (dto.getAvailableCopies() != null) b.setAvailableCopies(dto.getAvailableCopies());
        return toDTO(bookRepository.save(b));
    }

    @Override
    @Transactional(readOnly = true)
    public BookDTO getBook(Long id) {
        return bookRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Book not found"));
    }

    @Override
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream().map(this::toDTO).collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> searchBooks(String term) {
        if (term == null || term.trim().isEmpty()) return getAllBooks();
        return bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(term, term, term)
                .stream().map(this::toDTO).collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCategory(String category) {
        return bookRepository.findByCategoryIgnoreCase(category).stream().map(this::toDTO).collect(toList());
    }

    @Override
    public BookBorrowingDTO borrowBook(BorrowRequestDTO dto) {

        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No available copies");
        }

        BookBorrowing bb = new BookBorrowing();
        bb.setBook(book);

        if (dto.getStudentId() != null) {
            Student student = studentRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            bb.setStudent(student);
        }

        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
            bb.setTeacher(teacher);
        }

        bb.setBorrowDate(LocalDate.now());
        bb.setDueDate(LocalDate.now().plusDays(14));
        bb.setStatus(BookBorrowing.BorrowingStatus.BORROWED);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return toDTO(borrowingRepository.save(bb));
    }

    @Override
    public BookBorrowingDTO returnBook(Long borrowingId) {
        BookBorrowing bb = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        // ✅ enum compare
        if (bb.getStatus() != BookBorrowing.BorrowingStatus.BORROWED &&
                bb.getStatus() != BookBorrowing.BorrowingStatus.RENEWED) {
            throw new RuntimeException("Borrowing is not active");
        }

        bb.setReturnDate(LocalDate.now());
        bb.setStatus(BookBorrowing.BorrowingStatus.RETURNED);

        Book book = bb.getBook();
        book.setAvailableCopies((book.getAvailableCopies() == null ? 0 : book.getAvailableCopies()) + 1);
        bookRepository.save(book);

        return toDTO(borrowingRepository.save(bb));
    }

    @Override
    public BookBorrowingDTO renewBook(Long borrowingId) {
        BookBorrowing bb = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        if (bb.getStatus() != BookBorrowing.BorrowingStatus.BORROWED &&
                bb.getStatus() != BookBorrowing.BorrowingStatus.RENEWED) {
            throw new RuntimeException("Borrowing is not active");
        }

        bb.setDueDate(bb.getDueDate().plusDays(7));
        bb.setStatus(BookBorrowing.BorrowingStatus.RENEWED);

        return toDTO(borrowingRepository.save(bb));
    }

    @Override
    public BookBorrowingDTO reportLost(Long borrowingId) {
        BookBorrowing bb = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        bb.setStatus(BookBorrowing.BorrowingStatus.LOST);

        // Optional: if book is lost you may decide NOT to increment available copies on return.
        return toDTO(borrowingRepository.save(bb));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getBorrowingsByStudent(Long studentId) {
        return borrowingRepository.findByStudentId(studentId).stream().map(this::toDTO).collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getBorrowingsByTeacher(Long teacherId) {
        return borrowingRepository.findByTeacherId(teacherId).stream().map(this::toDTO).collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getOverdueBorrowings() {
        // ✅ If status is enum, your repo method should be: findByStatus(BookBorrowing.BorrowingStatus status)
        // If you currently have findByStatusIgnoreCase(String) it will not match enum.
        return borrowingRepository
                .findByStatus(BookBorrowing.BorrowingStatus.OVERDUE)
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }
}