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

    
    
    
    @Transactional
    public void initTestData() {
        
        synchronized (this) {
            if (studentRepository.count() >= 3) {
                System.out.println("✅ 测试数据已存在，跳过初始化");
                return;
            }

            
            studentRepository.deleteAllInBatch();

            
            List<Student> testStudents = List.of(
                    new Student(UUID.randomUUID().toString(), "CARD001", "张三", "20210001"),
                    new Student(UUID.randomUUID().toString(), "CARD002", "李四", "20210002"),
                    new Student(UUID.randomUUID().toString(), "CARD003", "王五", "20210003")
            );

            studentRepository.saveAll(testStudents);
            studentRepository.flush(); 

            System.out.println("✅ 测试学生数据已重置");
            printAllStudents(); 
        }
    }

    public void printAllStudents() {
        System.out.println("=== 当前学生数据 ===");
        studentRepository.findAll().forEach(s ->
                System.out.printf("学生: %s, 卡号: [%s], 学号: %s%n",
                        s.getName(), s.getCardId(), s.getStudentId())
        );
    }
    
    public void checkStudentData() {
        System.out.println("当前学生数量: " + studentRepository.count());
        studentRepository.findAll().forEach(student ->
                System.out.println("学生: " + student.getName() + ", 卡号: " + student.getCardId())
        );
    }
}