package eclosia.eclosia_organization_service.student_grade.service;

import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import eclosia.eclosia_organization_service.academic_curriculum_subject.repository.AcademicCurriculumSubjectRepository;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.academic_period.repository.AcademicPeriodRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.student_grade.dto.CreateStudentGradeDto;
import eclosia.eclosia_organization_service.student_grade.dto.UpdateStudentGradeDto;
import eclosia.eclosia_organization_service.student_grade.entity.StudentGrade;
import eclosia.eclosia_organization_service.student_grade.repository.StudentGradeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class StudentGradeService {

    private final StudentGradeRepository repository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AcademicCurriculumSubjectRepository academicCurriculumSubjectRepository;

    @Transactional
    public StudentGrade create(CreateStudentGradeDto dto) {
        validateUniqueness(
                dto.getStudentEnrollmentId(),
                dto.getAcademicPeriodId(),
                dto.getAcademicCurriculumSubjectId(),
                null
        );

        Enrollment enrollment = resolveEnrollment(dto.getStudentEnrollmentId());
        AcademicPeriod academicPeriod = resolveAcademicPeriod(dto.getAcademicPeriodId());
        AcademicCurriculumSubject academicCurriculumSubject =
                resolveAcademicCurriculumSubject(dto.getAcademicCurriculumSubjectId());

        validateScore(dto.getScore(), academicCurriculumSubject);

        StudentGrade studentGrade = new StudentGrade();
        mapFromDto(
                studentGrade,
                enrollment,
                academicPeriod,
                academicCurriculumSubject,
                dto.getScore(),
                dto.getObservation(),
                dto.getPublished()
        );

        return repository.save(studentGrade);
    }

    public List<StudentGrade> findAll() {
        return repository.findAll();
    }

    public List<StudentGrade> findByStudentEnrollmentId(UUID studentEnrollmentId) {
        return repository.findByStudentEnrollment_Id(studentEnrollmentId);
    }

    public List<StudentGrade> findByAcademicPeriodId(UUID academicPeriodId) {
        return repository.findByAcademicPeriod_Id(academicPeriodId);
    }

    public List<StudentGrade> findByAcademicCurriculumSubjectId(UUID academicCurriculumSubjectId) {
        return repository.findByAcademicCurriculumSubject_Id(academicCurriculumSubjectId);
    }

    public List<StudentGrade> findByStudentEnrollmentIdAndAcademicPeriodId(
            UUID studentEnrollmentId,
            UUID academicPeriodId
    ) {
        return repository.findByStudentEnrollment_IdAndAcademicPeriod_Id(
                studentEnrollmentId,
                academicPeriodId
        );
    }

    public StudentGrade findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student grade not found"));
    }

    @Transactional
    public StudentGrade update(UUID id, UpdateStudentGradeDto dto) {
        StudentGrade studentGrade = findById(id);
        validateUniqueness(
                dto.getStudentEnrollmentId(),
                dto.getAcademicPeriodId(),
                dto.getAcademicCurriculumSubjectId(),
                id
        );

        Enrollment enrollment = resolveEnrollment(dto.getStudentEnrollmentId());
        AcademicPeriod academicPeriod = resolveAcademicPeriod(dto.getAcademicPeriodId());
        AcademicCurriculumSubject academicCurriculumSubject =
                resolveAcademicCurriculumSubject(dto.getAcademicCurriculumSubjectId());

        validateScore(dto.getScore(), academicCurriculumSubject);

        mapFromDto(
                studentGrade,
                enrollment,
                academicPeriod,
                academicCurriculumSubject,
                dto.getScore(),
                dto.getObservation(),
                dto.getPublished()
        );

        return repository.save(studentGrade);
    }

    public void delete(UUID id) {
        StudentGrade studentGrade = findById(id);
        repository.delete(studentGrade);
    }

    private void validateUniqueness(
            UUID studentEnrollmentId,
            UUID academicPeriodId,
            UUID academicCurriculumSubjectId,
            UUID excludeId
    ) {
        if (excludeId == null) {
            if (repository.existsByStudentEnrollment_IdAndAcademicPeriod_IdAndAcademicCurriculumSubject_Id(
                    studentEnrollmentId,
                    academicPeriodId,
                    academicCurriculumSubjectId
            )) {
                throw new BadRequestException(
                        "A grade already exists for this enrollment, period and curriculum subject"
                );
            }
            return;
        }

        if (repository.existsByStudentEnrollment_IdAndAcademicPeriod_IdAndAcademicCurriculumSubject_IdAndIdNot(
                studentEnrollmentId,
                academicPeriodId,
                academicCurriculumSubjectId,
                excludeId
        )) {
            throw new BadRequestException(
                    "A grade already exists for this enrollment, period and curriculum subject"
            );
        }
    }

    private void validateScore(BigDecimal score, AcademicCurriculumSubject academicCurriculumSubject) {
        if (score == null || academicCurriculumSubject.getMaximumPoints() == null) {
            return;
        }
        if (score.compareTo(academicCurriculumSubject.getMaximumPoints()) > 0) {
            throw new BadRequestException(
                    "Score cannot exceed the maximum points of the academic curriculum subject"
            );
        }
    }

    private void mapFromDto(
            StudentGrade studentGrade,
            Enrollment enrollment,
            AcademicPeriod academicPeriod,
            AcademicCurriculumSubject academicCurriculumSubject,
            BigDecimal score,
            String observation,
            Boolean published
    ) {
        studentGrade.setStudentEnrollment(enrollment);
        studentGrade.setAcademicPeriod(academicPeriod);
        studentGrade.setAcademicCurriculumSubject(academicCurriculumSubject);
        studentGrade.setScore(score);
        studentGrade.setObservation(observation);
        studentGrade.setPublished(published != null ? published : false);
    }

    private Enrollment resolveEnrollment(UUID studentEnrollmentId) {
        return enrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found"));
    }

    private AcademicPeriod resolveAcademicPeriod(UUID academicPeriodId) {
        return academicPeriodRepository.findById(academicPeriodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found"));
    }

    private AcademicCurriculumSubject resolveAcademicCurriculumSubject(UUID academicCurriculumSubjectId) {
        return academicCurriculumSubjectRepository.findById(academicCurriculumSubjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic curriculum subject not found"));
    }
}
