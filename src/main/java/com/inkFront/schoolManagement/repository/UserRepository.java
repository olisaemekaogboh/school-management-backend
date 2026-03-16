package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :email")
    Optional<User> findByUsernameOrEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.teacher.id = :teacherId")
    Optional<User> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT u FROM User u WHERE u.student.id = :studentId")
    Optional<User> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT u FROM User u WHERE u.parent.id = :parentId")
    Optional<User> findByParentId(@Param("parentId") Long parentId);
    Optional<User> findByEmailOrUsername(String email, String username);
}