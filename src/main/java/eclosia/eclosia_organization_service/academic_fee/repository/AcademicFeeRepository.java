package eclosia.eclosia_organization_service.academic_fee.repository;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.studentCategory sc
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code,
                     l.levelOrder, COALESCE(pi.displayOrder, -1), fc.code, sc.code
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
}
