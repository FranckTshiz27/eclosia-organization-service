package eclosia.eclosia_organization_service.academic_curriculum_subject.repository;

import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AcademicCurriculumSubjectRepository extends JpaRepository<AcademicCurriculumSubject, UUID> {

    List<AcademicCurriculumSubject> findByAcademicCurriculum_IdOrderByDisplayOrderAsc(UUID academicCurriculumId);

    @Query("""
            SELECT acs
            FROM AcademicCurriculumSubject acs
            JOIN FETCH acs.subject s
            LEFT JOIN FETCH s.subjectDomain
            LEFT JOIN FETCH s.subjectSubDomain sd
            LEFT JOIN FETCH sd.subjectDomain
            WHERE acs.academicCurriculum.id = :curriculumId
            ORDER BY acs.displayOrder ASC
            """)
    List<AcademicCurriculumSubject> findByCurriculumIdWithSubjectAndDomain(
            @Param("curriculumId") UUID curriculumId
    );

    List<AcademicCurriculumSubject> findBySubject_Id(UUID subjectId);

    boolean existsByAcademicCurriculum_IdAndSubject_Id(UUID academicCurriculumId, UUID subjectId);

    boolean existsByAcademicCurriculum_IdAndSubject_IdAndIdNot(
            UUID academicCurriculumId,
            UUID subjectId,
            UUID id
    );
}
