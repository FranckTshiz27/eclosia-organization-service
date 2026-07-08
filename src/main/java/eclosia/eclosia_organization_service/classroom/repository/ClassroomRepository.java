package eclosia.eclosia_organization_service.classroom.repository;

import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    List<Classroom> findBySchool_IdOrderByClassroomDesignation_DisplayOrderAsc(UUID schoolId);

    @Query("""
            SELECT c FROM Classroom c
            JOIN FETCH c.academicLevel al
            JOIN FETCH al.academicCycle ac
            WHERE c.school.id = :schoolId
            """)
    List<Classroom> findBySchoolIdWithLevelAndCycle(@Param("schoolId") UUID schoolId);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Classroom c
            WHERE c.school.id = :schoolId
              AND c.academicLevel.id = :academicLevelId
              AND c.classroomDesignation.id = :classroomDesignationId
              AND ((:academicSectionId IS NULL AND c.academicSection IS NULL)
                   OR c.academicSection.id = :academicSectionId)
              AND ((:academicOptionId IS NULL AND c.academicOption IS NULL)
                   OR c.academicOption.id = :academicOptionId)
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    boolean existsDuplicate(
            @Param("schoolId") UUID schoolId,
            @Param("academicLevelId") UUID academicLevelId,
            @Param("academicSectionId") UUID academicSectionId,
            @Param("academicOptionId") UUID academicOptionId,
            @Param("classroomDesignationId") UUID classroomDesignationId,
            @Param("excludeId") UUID excludeId
    );
}
