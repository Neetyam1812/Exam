package University.exam.service;

import University.exam.Entity.Student;
import University.exam.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Fetch a student by their unique enrollment number.
     * @param enrollmentNo The enrollment number of the student.
     * @return An Optional containing the Student if found, or empty otherwise.
     */
    public Optional<Student> getStudentByEnrollmentNo(String enrollmentNo) {
        return studentRepository.findByEnrollmentNo(enrollmentNo);
    }

    /**
     * Save a student to the database.
     * @param student The student object to save.
     * @return The persisted student entity.
     */
    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }
}
