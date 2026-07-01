package eclosia.eclosia_organization_service.student.repository;

import eclosia.eclosia_organization_service.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByStudentNumberAndIdNot(String studentNumber, UUID id);
}
