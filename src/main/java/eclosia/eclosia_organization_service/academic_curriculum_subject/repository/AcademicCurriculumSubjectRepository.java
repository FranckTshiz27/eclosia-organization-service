package eclosia.eclosia_organization_service.academic_curriculum_subject.repository;

import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicCurriculumSubjectRepository extends JpaRepository<AcademicCurriculumSubject, UUID> {

    List<AcademicCurriculumSubject> findByAcademicCurriculum_IdOrderByDisplayOrderAsc(UUID academicCurriculumId);

    List<AcademicCurriculumSubject> findBySubject_Id(UUID subjectId);

    boolean existsByAcademicCurriculum_IdAndSubject_Id(UUID academicCurriculumId, UUID subjectId);

    boolean existsByAcademicCurriculum_IdAndSubject_IdAndIdNot(
            UUID academicCurriculumId,
            UUID subjectId,
            UUID id
    );
}
