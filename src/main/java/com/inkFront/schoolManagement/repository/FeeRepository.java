package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    @Override
    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findAll();

    @Override
    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    Optional<Fee> findById(Long id);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findByStudentAndSessionAndTerm(Student student, String session, Fee.Term term);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findByStudent(Student student);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findBySessionAndTerm(String session, Fee.Term term);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findByStatus(Fee.PaymentStatus status);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Fee> findByStatusIn(List<Fee.PaymentStatus> statuses);

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    Optional<Fee> findByStudentAndSessionAndTermAndFeeType(
            Student student,
            String session,
            Fee.Term term,
            Fee.FeeType feeType
    );

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.status IN ('PENDING', 'PARTIAL', 'OVERDUE')
          AND f.dueDate < :date
    """)
    List<Fee> findOverdueFees(@Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(f)
        FROM Fee f
        WHERE f.status = 'OVERDUE'
          AND f.session = :session
          AND f.term = :term
    """)
    long countOverdueFeesBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.status = 'PENDING'
          AND f.dueDate BETWEEN :start AND :end
    """)
    List<Fee> findUpcomingFees(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.status = 'PENDING'
          AND f.dueDate BETWEEN :start AND :end
          AND f.session = :session
          AND f.term = :term
    """)
    List<Fee> findUpcomingFeesBySessionAndTerm(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT SUM(f.amount)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
    """)
    Double getTotalExpectedBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT SUM(f.paidAmount)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
    """)
    Double getTotalCollectedBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT SUM(f.balance)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
          AND f.status <> 'PAID'
    """)
    Double getTotalOutstandingBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT COUNT(f)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
    """)
    Long getTotalFeesCountBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT COUNT(DISTINCT f.student)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
    """)
    Long getTotalStudentsWithFeesBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT f.status, COUNT(f)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
        GROUP BY f.status
    """)
    List<Object[]> countFeesByStatus(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT f.student, SUM(f.balance)
        FROM Fee f
        WHERE f.status <> 'PAID'
          AND f.session = :session
          AND f.term = :term
        GROUP BY f.student
        HAVING SUM(f.balance) > 0
    """)
    List<Object[]> getStudentsWithOutstandingBalance(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT f.student.schoolClass.className,
               COUNT(DISTINCT f.student),
               SUM(f.amount),
               SUM(f.paidAmount)
        FROM Fee f
        WHERE f.session = :session
          AND f.term = :term
        GROUP BY f.student.schoolClass.className
    """)
    List<Object[]> getClassWiseSummary(
            @Param("session") String session,
            @Param("term") Fee.Term term
    );

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.student = :student
          AND f.paidAmount > 0
        ORDER BY f.paidDate DESC
    """)
    List<Fee> findPaymentHistoryByStudent(@Param("student") Student student);

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.paidDate BETWEEN :startDate AND :endDate
        ORDER BY f.paidDate DESC
    """)
    List<Fee> findPaymentsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT f
        FROM Fee f
        JOIN FETCH f.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE f.reminderCount < :maxReminders
          AND f.status IN ('PENDING', 'PARTIAL', 'OVERDUE')
    """)
    List<Fee> findFeesNeedingReminders(@Param("maxReminders") int maxReminders);

    @Query("""
        SELECT YEAR(f.paidDate), MONTH(f.paidDate), SUM(f.paidAmount)
        FROM Fee f
        WHERE f.paidDate IS NOT NULL
          AND f.status = 'PAID'
        GROUP BY YEAR(f.paidDate), MONTH(f.paidDate)
        ORDER BY YEAR(f.paidDate) DESC, MONTH(f.paidDate) DESC
    """)
    List<Object[]> getMonthlyCollectionReport();

    @Query("""
        SELECT COUNT(f)
        FROM Fee f
        WHERE f.dueDate BETWEEN :start AND :end
          AND f.status <> 'PAID'
    """)
    long countDueFeesInRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}