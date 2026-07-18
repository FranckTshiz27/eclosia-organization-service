package eclosia.eclosia_organization_service.academic_curriculum.repository;

import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AcademicCurriculumRepository extends JpaRepository<AcademicCurriculum, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM AcademicCurriculum c
            WHERE c.academicYear.id = :academicYearId
              AND c.academicCycle.id = :academicCycleId
              AND c.academicLevel.id = :academicLevelId
              AND ((:academicSectionId IS NULL AND c.academicSection IS NULL)
                   OR c.academicSection.id = :academicSectionId)
              AND ((:academicOptionId IS NULL AND c.academicOption IS NULL)
                   OR c.academicOption.id = :academicOptionId)
            """)
    boolean existsByCurriculumKeys(
            @Param("academicYearId") UUID academicYearId,
            @Param("academicCycleId") UUID academicCycleId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId
    );

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM AcademicCurriculum c
            WHERE c.academicYear.id = :academicYearId
              AND c.academicCycle.id = :academicCycleId
              AND c.academicLevel.id = :academicLevelId
              AND ((:academicSectionId IS NULL AND c.academicSection IS NULL)
                   OR c.academicSection.id = :academicSectionId)
              AND ((:academicOptionId IS NULL AND c.academicOption IS NULL)
                   OR c.academicOption.id = :academicOptionId)
              AND c.id <> :id
            """)
    boolean existsByCurriculumKeysAndIdNot(
            @Param("academicYearId") UUID academicYearId,
            @Param("academicCycleId") UUID academicCycleId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId,
            @Param("id") UUID id
    );

    List<AcademicCurriculum> findByAcademicYear_Id(UUID academicYearId);

    List<AcademicCurriculum> findByAcademicYear_Country_Id(UUID countryId);

    List<AcademicCurriculum> findByAcademicCycle_Id(UUID academicCycleId);

    List<AcademicCurriculum> findByAcademicLevel_Id(UUID academicLevelId);
}
