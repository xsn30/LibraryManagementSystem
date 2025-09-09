package com.example.qflslibrarymanagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student addStudent(Student student) {
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentByCardId(String cardId) {
        return studentRepository.findByCardId(cardId);
    }

    public Optional<Student> getStudentById(String id) {
        return studentRepository.findById(id);
    }

    // 初始化测试学生数据
    public void initTestData() {
        if (studentRepository.count() == 0) {
            Student student = new Student(
                    UUID.randomUUID().toString(),
                    "CARD001",  // 测试饭卡ID
                    "测试学生",
                    "20210001"  // 测试学号
            );
            studentRepository.save(student);
            System.out.println("创建测试学生数据完成");
        }
    }
}