package eclosia.eclosia_organization_service.classroom_designation.service;

import eclosia.eclosia_organization_service.classroom_designation.dto.CreateClassroomDesignationDto;
import eclosia.eclosia_organization_service.classroom_designation.dto.UpdateClassroomDesignationDto;
import eclosia.eclosia_organization_service.classroom_designation.entity.ClassroomDesignation;
import eclosia.eclosia_organization_service.classroom_designation.repository.ClassroomDesignationRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class ClassroomDesignationService {

    private final ClassroomDesignationRepository repository;
    private final SchoolRepository schoolRepository;

    public ClassroomDesignation create(CreateClassroomDesignationDto dto) {
        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), null);

        ClassroomDesignation designation = new ClassroomDesignation();
        mapFromDto(designation, dto.getCode(), dto.getName(), dto.getDisplayOrder(),
                dto.getActive(), dto.getDescription(), dto.getSchoolId());
        return repository.save(designation);
    }

    public List<ClassroomDesignation> findAll() {
        return repository.findAll();
    }

    public List<ClassroomDesignation> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByDisplayOrderAsc(schoolId);
    }

    public ClassroomDesignation findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom designation not found"));
    }

    public ClassroomDesignation update(UUID id, UpdateClassroomDesignationDto dto) {
        ClassroomDesignation designation = findById(id);

        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), id);

        mapFromDto(designation, dto.getCode(), dto.getName(), dto.getDisplayOrder(),
                dto.getActive(), dto.getDescription(), dto.getSchoolId());
        return repository.save(designation);
    }

    public void delete(UUID id) {
        ClassroomDesignation designation = findById(id);
        repository.delete(designation);
    }

    private void validateUniqueness(UUID schoolId, String code, String name, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCode(schoolId, code)) {
                throw new BadRequestException("Designation code already exists for this school");
            }
            if (repository.existsBySchool_IdAndName(schoolId, name)) {
                throw new BadRequestException("Designation name already exists for this school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCodeAndIdNot(schoolId, code, excludeId)) {
            throw new BadRequestException("Designation code already exists for this school");
        }
        if (repository.existsBySchool_IdAndNameAndIdNot(schoolId, name, excludeId)) {
            throw new BadRequestException("Designation name already exists for this school");
        }
    }

    private void mapFromDto(
            ClassroomDesignation designation,
            String code,
            String name,
            Integer displayOrder,
            Boolean active,
            String description,
            UUID schoolId
    ) {
        designation.setCode(code);
        designation.setName(name);
        designation.setDisplayOrder(displayOrder);
        designation.setActive(active != null ? active : true);
        designation.setDescription(description);
        designation.setSchool(resolveSchool(schoolId));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
