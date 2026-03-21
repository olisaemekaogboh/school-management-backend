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
import com.inkFront.schoolManagement.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional
public class LibraryServiceImpl implements LibraryService {

    private final BookRepository bookRepository;
    private final BookBorrowingRepository borrowingRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    private BookDTO toDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPublisher(book.getPublisher());
        dto.setPublicationDate(
                book.getPublicationDate() != null ? book.getPublicationDate().toString() : null
        );
        dto.setEdition(book.getEdition());
        dto.setCategory(book.getCategory());
        dto.setShelfLocation(book.getShelfLocation());
        dto.setDescription(book.getDescription());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        dto.setStatus(book.getStatus() != null ? book.getStatus().name() : null);
        return dto;
    }

    private BookBorrowingDTO toDTO(BookBorrowing borrowing) {
        BookBorrowingDTO dto = new BookBorrowingDTO();
        dto.setId(borrowing.getId());

        if (borrowing.getBook() != null) {
            dto.setBookId(borrowing.getBook().getId());
            dto.setBookTitle(borrowing.getBook().getTitle());
        }

        if (borrowing.getStudent() != null) {
            dto.setStudentId(borrowing.getStudent().getId());
            dto.setStudentAdmissionNumber(borrowing.getStudent().getAdmissionNumber());
            dto.setStudentName(
                    ((borrowing.getStudent().getFirstName() != null ? borrowing.getStudent().getFirstName() : "") + " " +
                            (borrowing.getStudent().getLastName() != null ? borrowing.getStudent().getLastName() : ""))
                            .replaceAll("\\s+", " ")
                            .trim()
            );
        }

        if (borrowing.getTeacher() != null) {
            dto.setTeacherId(borrowing.getTeacher().getId());
            dto.setTeacherEmployeeId(
                    borrowing.getTeacher().getEmployeeId() != null
                            ? borrowing.getTeacher().getEmployeeId()
                            : borrowing.getTeacher().getTeacherId()
            );
            dto.setTeacherName(
                    ((borrowing.getTeacher().getFirstName() != null ? borrowing.getTeacher().getFirstName() : "") + " " +
                            (borrowing.getTeacher().getLastName() != null ? borrowing.getTeacher().getLastName() : ""))
                            .replaceAll("\\s+", " ")
                            .trim()
            );
        }

        dto.setBorrowDate(borrowing.getBorrowDate() != null ? borrowing.getBorrowDate().toString() : null);
        dto.setDueDate(borrowing.getDueDate() != null ? borrowing.getDueDate().toString() : null);
        dto.setReturnDate(borrowing.getReturnDate() != null ? borrowing.getReturnDate().toString() : null);
        dto.setStatus(borrowing.getStatus() != null ? borrowing.getStatus().name() : null);
        dto.setRemarks(borrowing.getRemarks());

        return dto;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String buildRenewalRemark(String currentRemarks) {
        String oldRemarks = currentRemarks == null ? "" : currentRemarks.trim();
        String renewalNote = "Renewed on " + LocalDate.now();
        return oldRemarks.isEmpty() ? renewalNote : oldRemarks + " | " + renewalNote;
    }

    private void validateCopies(Integer totalCopies, Integer availableCopies) {
        int total = totalCopies == null ? 0 : totalCopies;
        int available = availableCopies == null ? total : availableCopies;

        if (total < 0) {
            throw new RuntimeException("Total copies cannot be negative");
        }

        if (available < 0) {
            throw new RuntimeException("Available copies cannot be negative");
        }

        if (available > total) {
            throw new RuntimeException("Available copies cannot be greater than total copies");
        }
    }

    private void updateBookStatusFromCopies(Book book) {
        int available = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();

        if (book.getStatus() == Book.BookStatus.LOST) {
            return;
        }

        book.setStatus(available > 0 ? Book.BookStatus.AVAILABLE : Book.BookStatus.BORROWED);
    }

    @Override
    public BookDTO createBook(BookDTO dto) {
        validateCopies(dto.getTotalCopies(), dto.getAvailableCopies());

        Book book = new Book();
        book.setTitle(normalizeText(dto.getTitle()));
        book.setAuthor(normalizeText(dto.getAuthor()));
        book.setIsbn(normalizeText(dto.getIsbn()));
        book.setPublisher(normalizeText(dto.getPublisher()));
        book.setPublicationDate(parseDate(dto.getPublicationDate()));
        book.setEdition(normalizeText(dto.getEdition()));
        book.setCategory(normalizeText(dto.getCategory()));
        book.setShelfLocation(normalizeText(dto.getShelfLocation()));
        book.setDescription(normalizeText(dto.getDescription()));

        int totalCopies = dto.getTotalCopies() == null ? 0 : dto.getTotalCopies();
        int availableCopies = dto.getAvailableCopies() == null ? totalCopies : dto.getAvailableCopies();

        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        updateBookStatusFromCopies(book);

        return toDTO(bookRepository.save(book));
    }

    @Override
    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Integer nextTotalCopies = dto.getTotalCopies() != null ? dto.getTotalCopies() : book.getTotalCopies();
        Integer nextAvailableCopies = dto.getAvailableCopies() != null ? dto.getAvailableCopies() : book.getAvailableCopies();

        validateCopies(nextTotalCopies, nextAvailableCopies);

        book.setTitle(normalizeText(dto.getTitle()));
        book.setAuthor(normalizeText(dto.getAuthor()));
        book.setIsbn(normalizeText(dto.getIsbn()));
        book.setPublisher(normalizeText(dto.getPublisher()));
        book.setPublicationDate(parseDate(dto.getPublicationDate()));
        book.setEdition(normalizeText(dto.getEdition()));
        book.setCategory(normalizeText(dto.getCategory()));
        book.setShelfLocation(normalizeText(dto.getShelfLocation()));
        book.setDescription(normalizeText(dto.getDescription()));
        book.setTotalCopies(nextTotalCopies);
        book.setAvailableCopies(nextAvailableCopies);
        updateBookStatusFromCopies(book);

        return toDTO(bookRepository.save(book));
    }

    @Override
    @Transactional(readOnly = true)
    public BookDTO getBook(Long id) {
        return bookRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Book not found"));
    }

    @Override
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        boolean hasActiveBorrowing = borrowingRepository.findAll().stream()
                .anyMatch(b ->
                        b.getBook() != null &&
                                b.getBook().getId().equals(id) &&
                                (b.getStatus() == BookBorrowing.BorrowingStatus.BORROWED ||
                                        b.getStatus() == BookBorrowing.BorrowingStatus.OVERDUE)
                );

        if (hasActiveBorrowing) {
            throw new RuntimeException("Cannot delete a book with active borrowings");
        }

        bookRepository.delete(book);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> searchBooks(String term) {
        if (term == null || term.trim().isEmpty()) {
            return getAllBooks();
        }

        return bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
                        term.trim(), term.trim(), term.trim()
                )
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCategory(String category) {
        return bookRepository.findByCategoryIgnoreCase(category)
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    public BookBorrowingDTO borrowBook(BorrowRequestDTO dto) {
        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No available copies");
        }

        boolean hasStudentId = dto.getStudentId() != null;
        boolean hasTeacherId = dto.getTeacherId() != null;
        boolean hasStudentAdmissionNumber =
                dto.getStudentAdmissionNumber() != null &&
                        !dto.getStudentAdmissionNumber().trim().isEmpty();
        boolean hasTeacherEmployeeId =
                dto.getTeacherEmployeeId() != null &&
                        !dto.getTeacherEmployeeId().trim().isEmpty();

        boolean studentProvided = hasStudentId || hasStudentAdmissionNumber;
        boolean teacherProvided = hasTeacherId || hasTeacherEmployeeId;

        if (!studentProvided && !teacherProvided) {
            throw new RuntimeException("Provide either student admission number/studentId or teacher employeeId/teacherId");
        }

        if (studentProvided && teacherProvided) {
            throw new RuntimeException("Provide only one borrower: student or teacher");
        }

        BookBorrowing borrowing = new BookBorrowing();
        borrowing.setBook(book);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setRemarks(normalizeText(dto.getRemarks()));

        LocalDate dueDate = parseDate(dto.getDueDate());
        borrowing.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(14));

        if (borrowing.getDueDate().isBefore(borrowing.getBorrowDate())) {
            throw new RuntimeException("Due date cannot be before borrow date");
        }

        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);

        if (studentProvided) {
            Student student;
            if (hasStudentId) {
                student = studentRepository.findById(dto.getStudentId())
                        .orElseThrow(() -> new RuntimeException("Student not found"));
            } else {
                student = studentRepository.findByAdmissionNumber(dto.getStudentAdmissionNumber().trim())
                        .orElseThrow(() -> new RuntimeException(
                                "Student not found with admission number: " + dto.getStudentAdmissionNumber()
                        ));
            }

            borrowing.setStudent(student);
            borrowing.setTeacher(null);
        }

        if (teacherProvided) {
            Teacher teacher;
            if (hasTeacherId) {
                teacher = teacherRepository.findById(dto.getTeacherId())
                        .orElseThrow(() -> new RuntimeException("Teacher not found"));
            } else {
                String employeeId = dto.getTeacherEmployeeId().trim();

                Optional<Teacher> teacherByEmployeeId = teacherRepository.findByEmployeeId(employeeId);
                if (teacherByEmployeeId.isPresent()) {
                    teacher = teacherByEmployeeId.get();
                } else {
                    teacher = teacherRepository.findByTeacherId(employeeId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Teacher not found with employee ID: " + dto.getTeacherEmployeeId()
                            ));
                }
            }

            borrowing.setTeacher(teacher);
            borrowing.setStudent(null);
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        updateBookStatusFromCopies(book);
        bookRepository.save(book);

        return toDTO(borrowingRepository.save(borrowing));
    }

    @Override
    public BookBorrowingDTO returnBook(Long borrowingId) {
        BookBorrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        if (borrowing.getStatus() != BookBorrowing.BorrowingStatus.BORROWED &&
                borrowing.getStatus() != BookBorrowing.BorrowingStatus.OVERDUE) {
            throw new RuntimeException("Borrowing is not active");
        }

        borrowing.setReturnDate(LocalDate.now());
        borrowing.setStatus(BookBorrowing.BorrowingStatus.RETURNED);

        Book book = borrowing.getBook();
        if (book != null) {
            int available = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();
            int total = book.getTotalCopies() == null ? 0 : book.getTotalCopies();

            available = Math.min(available + 1, total);
            book.setAvailableCopies(available);
            updateBookStatusFromCopies(book);
            bookRepository.save(book);
        }

        return toDTO(borrowingRepository.save(borrowing));
    }

    @Override
    public BookBorrowingDTO renewBook(Long borrowingId) {
        BookBorrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        if (borrowing.getStatus() != BookBorrowing.BorrowingStatus.BORROWED &&
                borrowing.getStatus() != BookBorrowing.BorrowingStatus.OVERDUE) {
            throw new RuntimeException("Borrowing is not active");
        }

        LocalDate baseDate = borrowing.getDueDate() != null ? borrowing.getDueDate() : LocalDate.now();
        borrowing.setDueDate(baseDate.plusDays(7));
        borrowing.setRemarks(buildRenewalRemark(borrowing.getRemarks()));

        // Keep BORROWED because your database constraint does not allow RENEWED
        borrowing.setStatus(BookBorrowing.BorrowingStatus.BORROWED);

        return toDTO(borrowingRepository.save(borrowing));
    }

    @Override
    public BookBorrowingDTO reportLost(Long borrowingId) {
        BookBorrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new RuntimeException("Borrowing not found"));

        if (borrowing.getStatus() == BookBorrowing.BorrowingStatus.RETURNED) {
            throw new RuntimeException("Returned borrowing cannot be marked as lost");
        }

        borrowing.setStatus(BookBorrowing.BorrowingStatus.LOST);

        Book book = borrowing.getBook();
        if (book != null) {
            int available = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();
            if (available > 0) {
                book.setStatus(Book.BookStatus.AVAILABLE);
            } else {
                book.setStatus(Book.BookStatus.LOST);
            }
            bookRepository.save(book);
        }

        return toDTO(borrowingRepository.save(borrowing));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getAllBorrowings() {
        return borrowingRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getBorrowingsByStudent(Long studentId) {
        return borrowingRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowingDTO> getBorrowingsByTeacher(Long teacherId) {
        return borrowingRepository.findByTeacher_Id(teacherId)
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    public List<BookBorrowingDTO> getOverdueBorrowings() {
        List<BookBorrowing> activeOverdue = borrowingRepository.findByStatusAndDueDateBefore(
                BookBorrowing.BorrowingStatus.BORROWED,
                LocalDate.now()
        );

        for (BookBorrowing borrowing : activeOverdue) {
            borrowing.setStatus(BookBorrowing.BorrowingStatus.OVERDUE);
        }

        if (!activeOverdue.isEmpty()) {
            borrowingRepository.saveAll(activeOverdue);
        }

        return borrowingRepository.findByStatus(BookBorrowing.BorrowingStatus.OVERDUE)
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLibraryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Book> books = bookRepository.findAll();
        List<BookBorrowing> borrowings = borrowingRepository.findAll();

        long totalBooks = books.size();

        int totalCopies = books.stream()
                .map(Book::getTotalCopies)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        int availableCopies = books.stream()
                .map(Book::getAvailableCopies)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        long borrowedCount = borrowings.stream()
                .filter(b ->
                        b.getStatus() == BookBorrowing.BorrowingStatus.BORROWED ||
                                b.getStatus() == BookBorrowing.BorrowingStatus.OVERDUE
                )
                .count();

        long returnedCount = borrowings.stream()
                .filter(b -> b.getStatus() == BookBorrowing.BorrowingStatus.RETURNED)
                .count();

        long lostCount = borrowings.stream()
                .filter(b -> b.getStatus() == BookBorrowing.BorrowingStatus.LOST)
                .count();

        long overdueCount = borrowings.stream()
                .filter(b -> b.getStatus() == BookBorrowing.BorrowingStatus.OVERDUE)
                .count();

        stats.put("totalBooks", totalBooks);
        stats.put("totalCopies", totalCopies);
        stats.put("availableCopies", availableCopies);
        stats.put("borrowedCount", borrowedCount);
        stats.put("returnedCount", returnedCount);
        stats.put("lostCount", lostCount);
        stats.put("overdueCount", overdueCount);

        return stats;
    }
}