package eclosia.eclosia_organization_service.enrollment.repository;

import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByStudent_IdAndAcademicYear_Id(UUID studentId, UUID academicYearId);

    Page<Enrollment> findByAcademicYear_IdOrderByCreatedAtDesc(UUID academicYearId, Pageable pageable);

    Page<Enrollment> findByAcademicYear_IdAndAcademicYear_School_IdOrderByCreatedAtDesc(
            UUID academicYearId,
            UUID schoolId,
            Pageable pageable
    );

    Page<Enrollment> findByClassroom_IdOrderByCreatedAtDesc(UUID classroomId, Pageable pageable);

    Page<Enrollment> findByGuardian_IdOrderByCreatedAtDesc(UUID guardianId, Pageable pageable);

    Page<Enrollment> findByStudent_IdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN FETCH e.student s
            JOIN FETCH e.guardian
            JOIN FETCH e.classroom c
            JOIN FETCH c.classroomDesignation cd
            JOIN FETCH c.academicLevel al
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            JOIN FETCH e.academicYear ay
            LEFT JOIN FETCH e.photo
            WHERE ay.id = :academicYearId
              AND ay.school.id = :schoolId
            ORDER BY cd.displayOrder ASC,
                     al.levelOrder ASC,
                     sec.displayOrder ASC,
                     opt.displayOrder ASC,
                     s.lastName ASC,
                     s.firstName ASC
            """)
    List<Enrollment> findForSchoolAndAcademicYearReport(
            @Param("academicYearId") UUID academicYearId,
            @Param("schoolId") UUID schoolId
    );

    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN FETCH e.academicYear ay
            JOIN FETCH e.studentCategory sc
            JOIN FETCH e.classroom c
            JOIN FETCH c.academicLevel al
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            WHERE e.id = :id
            """)
    Optional<Enrollment> findByIdWithPaymentContext(@Param("id") UUID id);
}
