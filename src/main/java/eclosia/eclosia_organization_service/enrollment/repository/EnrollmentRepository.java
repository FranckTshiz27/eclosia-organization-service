package eclosia.eclosia_organization_service.enrollment.repository;

import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByStudent_IdAndAcademicYear_Id(UUID studentId, UUID academicYearId);

    List<Enrollment> findByAcademicYear_IdOrderByCreatedAtDesc(UUID academicYearId);

    List<Enrollment> findByClassroom_IdOrderByCreatedAtDesc(UUID classroomId);

    List<Enrollment> findByGuardian_IdOrderByCreatedAtDesc(UUID guardianId);

    List<Enrollment> findByStudent_IdOrderByCreatedAtDesc(UUID studentId);
}
