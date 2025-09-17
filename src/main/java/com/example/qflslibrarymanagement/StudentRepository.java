package com.example.qflslibrarymanagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {
    @Query("SELECT s FROM Student s WHERE TRIM(BOTH FROM s.cardId) = TRIM(BOTH FROM :cardId)")
    Optional<Student> findByCardId(@Param("cardId") String cardId);

    // 精确匹配查询（调试用）
    @Query(value = "SELECT * FROM students WHERE card_id = ?1", nativeQuery = true)
    Optional<Student> findByCardIdExact(String cardId);

    @Query("SELECT s.cardId FROM Student s")
    List<String> findAllCardIds();
}