package eclosia.eclosia_organization_service.academic_fee.repository;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicFeeRepository extends JpaRepository<AcademicFee, UUID> {

    @Query("""
            SELECT f FROM AcademicFee f
            JOIN FETCH f.academicCycle c
            JOIN FETCH f.academicLevel l
            LEFT JOIN FETCH f.paymentInstallment pi
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.studentCategory sc
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code,
                     l.levelOrder, COALESCE(pi.displayOrder, -1), fc.code, sc.code
            """)
    List<AcademicFee> findAllOrdered();

    @Query("""
            SELECT f FROM AcademicFee f
            JOIN FETCH f.academicCycle c
            JOIN FETCH f.academicLevel l
            LEFT JOIN FETCH f.paymentInstallment pi
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.studentCategory sc
            WHERE f.school.id = :schoolId
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code,
                     l.levelOrder, COALESCE(pi.displayOrder, -1), fc.code, sc.code
            """)
    List<AcademicFee> findBySchoolIdOrdered(@Param("schoolId") UUID schoolId);

    @Query("""
            SELECT f FROM AcademicFee f
            JOIN FETCH f.academicCycle c
            JOIN FETCH f.academicLevel l
            LEFT JOIN FETCH f.paymentInstallment pi
            LEFT JOIN FETCH f.academicSection sec
            LEFT JOIN FETCH f.academicOption opt
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.studentCategory sc
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code,
                     fc.code, l.levelOrder, sc.code, COALESCE(pi.displayOrder, -1)
            """)
    List<AcademicFee> findBySchoolIdAndAcademicYearIdOrdered(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM AcademicFee f
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
              AND f.feeCategory.id = :feeCategoryId
              AND f.academicCycle.id = :academicCycleId
              AND f.academicLevel.id = :academicLevelId
              AND f.studentCategory.id = :studentCategoryId
              AND ((:academicSectionId IS NULL AND f.academicSection IS NULL)
                   OR f.academicSection.id = :academicSectionId)
              AND ((:academicOptionId IS NULL AND f.academicOption IS NULL)
                   OR f.academicOption.id = :academicOptionId)
              AND ((:paymentInstallmentId IS NULL AND f.paymentInstallment IS NULL)
                   OR f.paymentInstallment.id = :paymentInstallmentId)
              AND (:excludeId IS NULL OR f.id <> :excludeId)
            """)
    boolean existsDuplicate(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("feeCategoryId") UUID feeCategoryId,
            @Param("academicCycleId") UUID academicCycleId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("studentCategoryId") UUID studentCategoryId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId,
            @Param("paymentInstallmentId") UUID paymentInstallmentId,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            SELECT f FROM AcademicFee f
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
              AND f.feeCategory.id = :feeCategoryId
              AND f.academicCycle.id = :academicCycleId
              AND f.academicLevel.id = :academicLevelId
              AND f.studentCategory.id = :studentCategoryId
              AND f.active = true
              AND ((:academicSectionId IS NULL AND f.academicSection IS NULL)
                   OR f.academicSection.id = :academicSectionId)
              AND ((:academicOptionId IS NULL AND f.academicOption IS NULL)
                   OR f.academicOption.id = :academicOptionId)
              AND ((:paymentInstallmentId IS NULL AND f.paymentInstallment IS NULL)
                   OR f.paymentInstallment.id = :paymentInstallmentId)
            """)
    Optional<AcademicFee> findMatchingForEnrollment(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("feeCategoryId") UUID feeCategoryId,
            @Param("academicCycleId") UUID academicCycleId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("studentCategoryId") UUID studentCategoryId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId,
            @Param("paymentInstallmentId") UUID paymentInstallmentId
    );

    @Query("""
            SELECT f FROM AcademicFee f
            JOIN FETCH f.academicCycle c
            JOIN FETCH f.academicLevel l
            LEFT JOIN FETCH f.paymentInstallment pi
            LEFT JOIN FETCH f.academicSection
            LEFT JOIN FETCH f.academicOption
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.studentCategory sc
            WHERE f.id IN :ids
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code,
                     l.levelOrder, COALESCE(pi.displayOrder, -1), fc.code, sc.code
            """)
    List<AcademicFee> findByIdInWithDetails(@Param("ids") Collection<UUID> ids);

    @Query(value = """
            SELECT f.id
            FROM academic_fees f
            INNER JOIN enrollments e ON e.id = :enrollmentId
            INNER JOIN classrooms c ON c.id = e.classroom_id
            INNER JOIN academic_levels al ON al.id = c.academic_level_id
            INNER JOIN academic_years ay ON ay.id = e.academic_year_id
            WHERE f.active = true
              AND e.student_category_id IS NOT NULL
              AND f.school_id = ay.school_id
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
            """, nativeQuery = true)
    List<UUID> findMatchingFeeIdsByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
            SELECT COUNT(f)
            FROM AcademicFee f
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
            """)
    long countBySchoolIdAndAcademicYearId(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT COUNT(DISTINCT f.paymentInstallment.id)
            FROM AcademicFee f
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
              AND f.paymentInstallment IS NOT NULL
            """)
    long countDistinctInstallmentsBySchoolIdAndAcademicYearId(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );
}
