package eclosia.eclosia_organization_service.academic_fee.repository;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AcademicFeeRepository extends JpaRepository<AcademicFee, UUID> {

    List<AcademicFee> findBySchool_IdOrderByCodeAsc(UUID schoolId);

    List<AcademicFee> findBySchool_IdAndAcademicYear_IdOrderByCodeAsc(UUID schoolId, UUID academicYearId);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM AcademicFee f
            WHERE f.school.id = :schoolId
              AND f.academicYear.id = :academicYearId
              AND f.academicCycle.id = :academicCycleId
              AND f.academicLevel.id = :academicLevelId
              AND f.code = :code
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
            @Param("academicCycleId") UUID academicCycleId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId,
            @Param("paymentInstallmentId") UUID paymentInstallmentId,
            @Param("code") String code,
            @Param("excludeId") UUID excludeId
    );
}
