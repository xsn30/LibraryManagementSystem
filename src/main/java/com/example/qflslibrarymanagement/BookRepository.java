package com.example.qflslibrarymanagement;

import org.springframework.data.jpa.repository.JpaRepository;

// 继承 JpaRepository，自动拥有增删改查功能
public interface BookRepository extends JpaRepository<Book, String> {
}