package eclosia.eclosia_organization_service.student_period_score.repository;

import eclosia.eclosia_organization_service.student_period_score.entity.StudentPeriodScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentPeriodScoreRepository extends JpaRepository<StudentPeriodScore, UUID> {

    Optional<StudentPeriodScore> findByEnrollment_IdAndAcademicPeriod_IdAndAcademicCurriculumSubject_Id(
            UUID enrollmentId,
            UUID academicPeriodId,
            UUID academicCurriculumSubjectId
    );

    List<StudentPeriodScore> findByAcademicPeriod_IdAndAcademicCurriculumSubject_Id(
            UUID academicPeriodId,
            UUID academicCurriculumSubjectId
    );

    List<StudentPeriodScore> findByEnrollment_Id(UUID enrollmentId);
}
