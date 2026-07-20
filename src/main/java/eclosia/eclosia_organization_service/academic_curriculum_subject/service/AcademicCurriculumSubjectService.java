package eclosia.eclosia_organization_service.academic_curriculum_subject.service;

import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import eclosia.eclosia_organization_service.academic_curriculum.repository.AcademicCurriculumRepository;
import eclosia.eclosia_organization_service.academic_curriculum_subject.dto.CreateAcademicCurriculumSubjectDto;
import eclosia.eclosia_organization_service.academic_curriculum_subject.dto.UpdateAcademicCurriculumSubjectDto;
import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import eclosia.eclosia_organization_service.academic_curriculum_subject.repository.AcademicCurriculumSubjectRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject.repository.SubjectRepository;
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
public class AcademicCurriculumSubjectService {

    private final AcademicCurriculumSubjectRepository repository;
    private final AcademicCurriculumRepository academicCurriculumRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public AcademicCurriculumSubject create(CreateAcademicCurriculumSubjectDto dto) {
        validateUniqueness(dto.getAcademicCurriculumId(), dto.getSubjectId(), null);

        AcademicCurriculum academicCurriculum = resolveAcademicCurriculum(dto.getAcademicCurriculumId());
        Subject subject = resolveSubject(dto.getSubjectId());

        AcademicCurriculumSubject academicCurriculumSubject = new AcademicCurriculumSubject();
        mapFromDto(
                academicCurriculumSubject,
                academicCurriculum,
                subject,
                dto.getCoefficient(),
                dto.getMaximumPoints(),
                dto.getDisplayOrder(),
                dto.getMandatory(),
                dto.getActive()
        );

        return repository.save(academicCurriculumSubject);
    }

    public List<AcademicCurriculumSubject> findAll() {
        return repository.findAll();
    }

    public List<AcademicCurriculumSubject> findByAcademicCurriculumId(UUID academicCurriculumId) {
        return repository.findByAcademicCurriculum_IdOrderByDisplayOrderAsc(academicCurriculumId);
    }

    public List<AcademicCurriculumSubject> findBySubjectId(UUID subjectId) {
        return repository.findBySubject_Id(subjectId);
    }

    public AcademicCurriculumSubject findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic curriculum subject not found"));
    }

    @Transactional
    public AcademicCurriculumSubject update(UUID id, UpdateAcademicCurriculumSubjectDto dto) {
        AcademicCurriculumSubject academicCurriculumSubject = findById(id);
        validateUniqueness(dto.getAcademicCurriculumId(), dto.getSubjectId(), id);

        AcademicCurriculum academicCurriculum = resolveAcademicCurriculum(dto.getAcademicCurriculumId());
        Subject subject = resolveSubject(dto.getSubjectId());

        mapFromDto(
                academicCurriculumSubject,
                academicCurriculum,
                subject,
                dto.getCoefficient(),
                dto.getMaximumPoints(),
                dto.getDisplayOrder(),
                dto.getMandatory(),
                dto.getActive()
        );

        return repository.save(academicCurriculumSubject);
    }

    public void delete(UUID id) {
        AcademicCurriculumSubject academicCurriculumSubject = findById(id);
        repository.delete(academicCurriculumSubject);
    }

    private void validateUniqueness(UUID academicCurriculumId, UUID subjectId, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByAcademicCurriculum_IdAndSubject_Id(academicCurriculumId, subjectId)) {
                throw new BadRequestException("This subject is already linked to the academic curriculum");
            }
            return;
        }

        if (repository.existsByAcademicCurriculum_IdAndSubject_IdAndIdNot(
                academicCurriculumId,
                subjectId,
                excludeId
        )) {
            throw new BadRequestException("This subject is already linked to the academic curriculum");
        }
    }

    private void mapFromDto(
            AcademicCurriculumSubject academicCurriculumSubject,
            AcademicCurriculum academicCurriculum,
            Subject subject,
            BigDecimal coefficient,
            BigDecimal maximumPoints,
            Integer displayOrder,
            Boolean mandatory,
            Boolean active
    ) {
        academicCurriculumSubject.setAcademicCurriculum(academicCurriculum);
        academicCurriculumSubject.setSubject(subject);
        academicCurriculumSubject.setCoefficient(coefficient);
        academicCurriculumSubject.setMaximumPoints(maximumPoints);
        academicCurriculumSubject.setDisplayOrder(displayOrder);
        academicCurriculumSubject.setMandatory(mandatory != null ? mandatory : true);
        academicCurriculumSubject.setActive(active != null ? active : true);
    }

    private AcademicCurriculum resolveAcademicCurriculum(UUID academicCurriculumId) {
        return academicCurriculumRepository.findById(academicCurriculumId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic curriculum not found"));
    }

    private Subject resolveSubject(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }
}
