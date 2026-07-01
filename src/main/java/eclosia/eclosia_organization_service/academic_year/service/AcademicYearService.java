package eclosia.eclosia_organization_service.academic_year.service;

import eclosia.eclosia_organization_service.academic_year.dto.CreateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.dto.UpdateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_academic_model.entity.SchoolAcademicModel;
import eclosia.eclosia_organization_service.school_academic_model.repository.SchoolAcademicModelRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicYearService {

    private final AcademicYearRepository repository;
    private final SchoolRepository schoolRepository;
    private final SchoolAcademicModelRepository schoolAcademicModelRepository;

    @Transactional
    public AcademicYear create(CreateAcademicYearDto dto) {
        validateCodeUniqueness(dto.getSchoolId(), dto.getCode(), null);
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        School school = resolveSchool(dto.getSchoolId());
        SchoolAcademicModel schoolAcademicModel = resolveSchoolAcademicModel(dto.getSchoolAcademicModelId());
        validateSchoolAcademicModelOwnership(school.getId(), schoolAcademicModel);

        AcademicYear academicYear = new AcademicYear();
        mapFromDto(academicYear, dto.getCode(), dto.getStartDate(), dto.getEndDate(),
                dto.getCurrent(), dto.getStatus(), dto.getDescription(), school, schoolAcademicModel);

        deactivateOtherCurrentYears(school.getId(), null, academicYear.getCurrent());
        return repository.save(academicYear);
    }

    public List<AcademicYear> findAll() {
        return repository.findAll();
    }

    public List<AcademicYear> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByStartDateDesc(schoolId);
    }

    public List<AcademicYear> findBySchoolAcademicModelId(UUID schoolAcademicModelId) {
        return repository.findBySchoolAcademicModel_IdOrderByStartDateDesc(schoolAcademicModelId);
    }

    public AcademicYear findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    @Transactional
    public AcademicYear update(UUID id, UpdateAcademicYearDto dto) {
        AcademicYear academicYear = findById(id);

        validateCodeUniqueness(dto.getSchoolId(), dto.getCode(), id);
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        School school = resolveSchool(dto.getSchoolId());
        SchoolAcademicModel schoolAcademicModel = resolveSchoolAcademicModel(dto.getSchoolAcademicModelId());
        validateSchoolAcademicModelOwnership(school.getId(), schoolAcademicModel);

        mapFromDto(academicYear, dto.getCode(), dto.getStartDate(), dto.getEndDate(),
                dto.getCurrent(), dto.getStatus(), dto.getDescription(), school, schoolAcademicModel);

        deactivateOtherCurrentYears(school.getId(), id, academicYear.getCurrent());
        return repository.save(academicYear);
    }

    public void delete(UUID id) {
        AcademicYear academicYear = findById(id);
        repository.delete(academicYear);
    }

    private void validateCodeUniqueness(UUID schoolId, String code, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCode(schoolId, code)) {
                throw new BadRequestException("Academic year code already exists for this school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCodeAndIdNot(schoolId, code, excludeId)) {
            throw new BadRequestException("Academic year code already exists for this school");
        }
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be before or equal to end date");
        }
    }

    private void validateSchoolAcademicModelOwnership(UUID schoolId, SchoolAcademicModel schoolAcademicModel) {
        if (!schoolId.equals(schoolAcademicModel.getSchoolId())) {
            throw new BusinessException("This school academic model does not belong to this school");
        }
    }

    private void deactivateOtherCurrentYears(UUID schoolId, UUID currentId, Boolean current) {
        if (!Boolean.TRUE.equals(current)) {
            return;
        }

        List<AcademicYear> otherCurrentYears = currentId == null
                ? repository.findBySchool_IdAndCurrentTrue(schoolId)
                : repository.findBySchool_IdAndCurrentTrueAndIdNot(schoolId, currentId);

        otherCurrentYears.forEach(year -> year.setCurrent(false));
        repository.saveAll(otherCurrentYears);
    }

    private void mapFromDto(
            AcademicYear academicYear,
            String code,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Boolean current,
            eclosia.eclosia_organization_service.academic_year.entity.AcademicYearStatus status,
            String description,
            School school,
            SchoolAcademicModel schoolAcademicModel
    ) {
        academicYear.setCode(code);
        academicYear.setStartDate(startDate);
        academicYear.setEndDate(endDate);
        academicYear.setCurrent(current != null ? current : false);
        academicYear.setStatus(status);
        academicYear.setDescription(description);
        academicYear.setSchool(school);
        academicYear.setSchoolAcademicModel(schoolAcademicModel);
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private SchoolAcademicModel resolveSchoolAcademicModel(UUID schoolAcademicModelId) {
        return schoolAcademicModelRepository.findById(schoolAcademicModelId)
                .orElseThrow(() -> new ResourceNotFoundException("School academic model not found"));
    }
}
