package com.example.qflslibrarymanagement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByCardId(String cardId);
    Optional<Student> findByStudentId(String studentId);
}