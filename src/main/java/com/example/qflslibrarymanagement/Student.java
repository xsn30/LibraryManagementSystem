package com.example.qflslibrarymanagement;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String cardId;  // 饭卡ID

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String studentId; // 学号

    // 构造方法
    public Student() {}

    public Student(String id, String cardId, String name, String studentId) {
        this.id = id;
        this.cardId = cardId;
        this.name = name;
        this.studentId = studentId;
    }

    // Getter和Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}