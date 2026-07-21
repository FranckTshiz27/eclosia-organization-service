package eclosia.eclosia_organization_service.student_grade.repository;

import eclosia.eclosia_organization_service.student_grade.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, UUID> {

    List<StudentGrade> findByStudentEnrollment_Id(UUID studentEnrollmentId);

    List<StudentGrade> findByAcademicPeriod_Id(UUID academicPeriodId);

    List<StudentGrade> findByAcademicCurriculumSubject_Id(UUID academicCurriculumSubjectId);

    List<StudentGrade> findByStudentEnrollment_IdAndAcademicPeriod_Id(
            UUID studentEnrollmentId,
            UUID academicPeriodId
    );

    @Query("""
            SELECT g
            FROM StudentGrade g
            JOIN FETCH g.academicPeriod
            JOIN FETCH g.academicCurriculumSubject
            WHERE g.studentEnrollment.id IN :enrollmentIds
            """)
    List<StudentGrade> findByStudentEnrollment_IdIn(@Param("enrollmentIds") Collection<UUID> enrollmentIds);

    boolean existsByStudentEnrollment_IdAndAcademicPeriod_IdAndAcademicCurriculumSubject_Id(
            UUID studentEnrollmentId,
            UUID academicPeriodId,
            UUID academicCurriculumSubjectId
    );

    boolean existsByStudentEnrollment_IdAndAcademicPeriod_IdAndAcademicCurriculumSubject_IdAndIdNot(
            UUID studentEnrollmentId,
            UUID academicPeriodId,
            UUID academicCurriculumSubjectId,
            UUID id
    );
}
