package eclosia.eclosia_organization_service.school_academic_model.service;

import eclosia.eclosia_organization_service.academic_model.entity.AcademicModel;
import eclosia.eclosia_organization_service.academic_model.repository.AcademicModelRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_academic_model.dto.CreateSchoolAcademicModelDto;
import eclosia.eclosia_organization_service.school_academic_model.dto.UpdateSchoolAcademicModelDto;
import eclosia.eclosia_organization_service.school_academic_model.entity.SchoolAcademicModel;
import eclosia.eclosia_organization_service.school_academic_model.repository.SchoolAcademicModelRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Data
public class SchoolAcademicModelService {

    private final SchoolAcademicModelRepository repository;
    private final SchoolRepository schoolRepository;
    private final AcademicModelRepository academicModelRepository;

    @Transactional
    public SchoolAcademicModel create(CreateSchoolAcademicModelDto dto) {
        if (repository.existsBySchool_IdAndAcademicModel_IdAndStartDate(
                dto.getSchoolId(), dto.getAcademicModelId(), dto.getStartDate())) {
            throw new BadRequestException("This school academic model assignment already exists for this start date");
        }

        SchoolAcademicModel schoolAcademicModel = new SchoolAcademicModel();
        mapFromDto(schoolAcademicModel, dto.getSchoolId(), dto.getAcademicModelId(),
                dto.getStartDate(), dto.getEndDate(), dto.getActive(), dto.getComment());
        deactivateOtherActiveModels(dto.getSchoolId(), null, schoolAcademicModel.getActive());
        return repository.save(schoolAcademicModel);
    }

    public List<SchoolAcademicModel> findAll() {
        return repository.findAll();
    }

    public List<SchoolAcademicModel> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByStartDateDesc(schoolId);
    }

    public List<SchoolAcademicModel> findByAcademicModelId(UUID academicModelId) {
        return repository.findByAcademicModel_IdOrderByStartDateDesc(academicModelId);
    }

    public SchoolAcademicModel findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School academic model not found"));
    }

    @Transactional
    public SchoolAcademicModel update(UUID id, UpdateSchoolAcademicModelDto dto) {
        SchoolAcademicModel schoolAcademicModel = findById(id);

        if (repository.existsBySchool_IdAndAcademicModel_IdAndStartDateAndIdNot(
                dto.getSchoolId(), dto.getAcademicModelId(), dto.getStartDate(), id)) {
            throw new BadRequestException("This school academic model assignment already exists for this start date");
        }

        mapFromDto(schoolAcademicModel, dto.getSchoolId(), dto.getAcademicModelId(),
                dto.getStartDate(), dto.getEndDate(), dto.getActive(), dto.getComment());
        deactivateOtherActiveModels(dto.getSchoolId(), id, schoolAcademicModel.getActive());
        return repository.save(schoolAcademicModel);
    }

    public void delete(UUID id) {
        SchoolAcademicModel schoolAcademicModel = findById(id);
        repository.delete(schoolAcademicModel);
    }

    private void mapFromDto(
            SchoolAcademicModel schoolAcademicModel,
            UUID schoolId,
            UUID academicModelId,
            LocalDate startDate,
            LocalDate endDate,
            Boolean active,
            String comment
    ) {
        schoolAcademicModel.setSchool(resolveSchool(schoolId));
        schoolAcademicModel.setAcademicModel(resolveAcademicModel(academicModelId));
        schoolAcademicModel.setStartDate(startDate);
        schoolAcademicModel.setEndDate(endDate);
        schoolAcademicModel.setActive(active != null ? active : true);
        schoolAcademicModel.setComment(comment);
    }

    private void deactivateOtherActiveModels(UUID schoolId, UUID currentId, Boolean active) {
        if (!Boolean.TRUE.equals(active)) {
            return;
        }

        List<SchoolAcademicModel> otherActiveModels = currentId == null
                ? repository.findBySchool_IdAndActiveTrue(schoolId)
                : repository.findBySchool_IdAndActiveTrueAndIdNot(schoolId, currentId);

        otherActiveModels.forEach(model -> model.setActive(false));
        repository.saveAll(otherActiveModels);
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private AcademicModel resolveAcademicModel(UUID academicModelId) {
        return academicModelRepository.findById(academicModelId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic model not found"));
    }
}
