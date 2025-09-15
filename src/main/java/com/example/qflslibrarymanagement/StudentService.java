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
    // 初始化测试学生数据
    // 修改initTestData方法中的测试数据
    public void initTestData() {
        if (studentRepository.count() == 0) {
            Student student1 = new Student(
                    UUID.randomUUID().toString(),
                    "CARD001",  // 改为与提示一致
                    "张三",
                    "20210001"
            );

            Student student2 = new Student(
                    UUID.randomUUID().toString(),
                    "CARD002",   // 改为与提示一致
                    "李四",
                    "20210002"
            );

            Student student3 = new Student(
                    UUID.randomUUID().toString(),
                    "CARD003",      // 改为与提示一致
                    "王五",
                    "20210003"
            );

            studentRepository.save(student1);
            studentRepository.save(student2);
            studentRepository.save(student3);

            System.out.println("创建测试学生数据完成");
            System.out.println("可用测试卡号: CARD001, CARD002, CARD003"); // 与实际数据一致
        }
    }
    // 可以添加一个检查方法
    public void checkStudentData() {
        System.out.println("当前学生数量: " + studentRepository.count());
        studentRepository.findAll().forEach(student ->
                System.out.println("学生: " + student.getName() + ", 卡号: " + student.getCardId())
        );
    }
}