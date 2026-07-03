package eclosia.eclosia_organization_service.fee_category.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.fee_category.dto.CreateFeeCategoryDto;
import eclosia.eclosia_organization_service.fee_category.dto.UpdateFeeCategoryDto;
import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import eclosia.eclosia_organization_service.fee_category.repository.FeeCategoryRepository;
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
public class FeeCategoryService {

    private final FeeCategoryRepository repository;
    private final SchoolRepository schoolRepository;

    public FeeCategory create(CreateFeeCategoryDto dto) {
        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), null);

        FeeCategory feeCategory = new FeeCategory();
        mapFromDto(
                feeCategory,
                dto.getCode(),
                dto.getName(),
                dto.getDescription(),
                dto.getActive(),
                dto.getAllowInstallments(),
                dto.getComment(),
                dto.getSchoolId()
        );
        return repository.save(feeCategory);
    }

    public List<FeeCategory> findAll() {
        return repository.findAll();
    }

    public List<FeeCategory> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByNameAsc(schoolId);
    }

    public FeeCategory findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee category not found"));
    }

    public FeeCategory update(UUID id, UpdateFeeCategoryDto dto) {
        FeeCategory feeCategory = findById(id);

        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), id);

        mapFromDto(
                feeCategory,
                dto.getCode(),
                dto.getName(),
                dto.getDescription(),
                dto.getActive(),
                dto.getAllowInstallments(),
                dto.getComment(),
                dto.getSchoolId()
        );
        return repository.save(feeCategory);
    }

    public void delete(UUID id) {
        FeeCategory feeCategory = findById(id);
        repository.delete(feeCategory);
    }

    private void validateUniqueness(UUID schoolId, String code, String name, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCode(schoolId, code)) {
                throw new BadRequestException("Fee category code already exists for this school");
            }
            if (repository.existsBySchool_IdAndName(schoolId, name)) {
                throw new BadRequestException("Fee category name already exists for this school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCodeAndIdNot(schoolId, code, excludeId)) {
            throw new BadRequestException("Fee category code already exists for this school");
        }
        if (repository.existsBySchool_IdAndNameAndIdNot(schoolId, name, excludeId)) {
            throw new BadRequestException("Fee category name already exists for this school");
        }
    }

    private void mapFromDto(
            FeeCategory feeCategory,
            String code,
            String name,
            String description,
            Boolean active,
            Boolean allowInstallments,
            String comment,
            UUID schoolId
    ) {
        feeCategory.setCode(code);
        feeCategory.setName(name);
        feeCategory.setDescription(description);
        feeCategory.setActive(active != null ? active : true);
        feeCategory.setAllowInstallments(allowInstallments != null ? allowInstallments : true);
        feeCategory.setComment(comment);
        feeCategory.setSchool(resolveSchool(schoolId));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
