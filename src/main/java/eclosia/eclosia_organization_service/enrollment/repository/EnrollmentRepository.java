package eclosia.eclosia_organization_service.enrollment.repository;

import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.finance.projection.EnrollmentExpectedProjection;
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

    Page<Enrollment> findByAcademicYear_IdAndClassroom_School_IdOrderByCreatedAtDesc(
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
            JOIN FETCH al.academicCycle ac
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            JOIN FETCH e.academicYear ay
            LEFT JOIN FETCH e.photo
            WHERE ay.id = :academicYearId
              AND c.school.id = :schoolId
            ORDER BY COALESCE(ac.displayOrder, 2147483647), ac.code,
                     cd.displayOrder ASC,
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
            JOIN FETCH e.student
            JOIN FETCH e.academicYear ay
            JOIN FETCH e.studentCategory sc
            JOIN FETCH e.classroom c
            JOIN FETCH c.academicLevel al
            JOIN FETCH al.academicCycle
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            WHERE e.id = :id
            """)
    Optional<Enrollment> findByIdWithPaymentContext(@Param("id") UUID id);

    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN FETCH e.student s
            JOIN FETCH e.studentCategory sc
            JOIN FETCH e.academicYear ay
            JOIN FETCH e.classroom c
            JOIN FETCH c.academicLevel al
            JOIN FETCH al.academicCycle
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            WHERE ay.id = :academicYearId
              AND c.school.id = :schoolId
              AND (
                  LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
                  OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
                  OR LOWER(COALESCE(s.middleName, '')) LIKE LOWER(CONCAT('%', :name, '%'))
                  OR LOWER(CONCAT(s.lastName, ' ', s.firstName)) LIKE LOWER(CONCAT('%', :name, '%'))
                  OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
              )
            ORDER BY s.lastName ASC, s.firstName ASC
            """)
    List<Enrollment> searchByStudentNameAndAcademicYearAndSchool(
            @Param("name") String name,
            @Param("academicYearId") UUID academicYearId,
            @Param("schoolId") UUID schoolId
    );

    @Query(value = """
            SELECT e.id AS enrollmentId,
                   e.classroom_id AS classroomId,
                   COALESCE(SUM(f.amount), 0) AS expectedAmount
            FROM enrollments e
            INNER JOIN classrooms c ON c.id = e.classroom_id
            INNER JOIN academic_levels al ON al.id = c.academic_level_id
            INNER JOIN academic_years ay ON ay.id = e.academic_year_id
            LEFT JOIN academic_fees f ON (
                f.active = true
                AND e.student_category_id IS NOT NULL
                AND f.school_id = c.school_id
                AND f.academic_year_id = e.academic_year_id
                AND f.student_category_id = e.student_category_id
                AND f.academic_cycle_id = al.academic_cycle_id
                AND f.academic_level_id = al.id
                AND f.academic_section_id IS NOT DISTINCT FROM c.academic_section_id
                AND f.academic_option_id IS NOT DISTINCT FROM c.academic_option_id
                AND (
                    f.academic_option_id IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM academic_options ao
                        WHERE ao.id = f.academic_option_id
                          AND ao.academic_section_id IS NOT DISTINCT FROM c.academic_section_id
                    )
                )
                AND (
                    NOT COALESCE(al.requires_section, false)
                    OR (
                        f.academic_section_id IS NOT NULL
                        AND c.academic_section_id IS NOT NULL
                    )
                )
                AND (
                    NOT COALESCE(al.requires_option, false)
                    OR (
                        f.academic_option_id IS NOT NULL
                        AND c.academic_option_id IS NOT NULL
                    )
                )
            )
            WHERE c.school_id = :schoolId
              AND ay.id = :academicYearId
              AND UPPER(e.status) = 'ACTIVE'
            GROUP BY e.id, e.classroom_id
            """, nativeQuery = true)
    List<EnrollmentExpectedProjection> sumExpectedAmountsByEnrollment(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            JOIN e.academicYear ay
            JOIN e.classroom c
            WHERE c.school.id = :schoolId
              AND ay.id = :academicYearId
              AND UPPER(e.status) = 'ACTIVE'
            """)
    long countActiveBySchoolAndAcademicYear(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT COUNT(DISTINCT e.classroom.id)
            FROM Enrollment e
            JOIN e.academicYear ay
            JOIN e.classroom c
            WHERE c.school.id = :schoolId
              AND ay.id = :academicYearId
              AND UPPER(e.status) = 'ACTIVE'
            """)
    long countDistinctActiveClassroomsBySchoolAndAcademicYear(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );
}
