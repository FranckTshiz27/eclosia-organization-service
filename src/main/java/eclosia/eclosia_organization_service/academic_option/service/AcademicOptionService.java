package eclosia.eclosia_organization_service.academic_option.service;

import eclosia.eclosia_organization_service.academic_option.dto.CreateAcademicOptionDto;
import eclosia.eclosia_organization_service.academic_option.dto.UpdateAcademicOptionDto;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_option.repository.AcademicOptionRepository;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_section.repository.AcademicSectionRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicOptionService {

    private final AcademicOptionRepository repository;
    private final AcademicSectionRepository academicSectionRepository;

    public AcademicOption create(CreateAcademicOptionDto dto) {
        if (repository.existsByAcademicSection_IdAndCode(dto.getAcademicSectionId(), dto.getCode())) {
            throw new BadRequestException("Academic option code already exists for this section");
        }

        AcademicOption academicOption = new AcademicOption();
        mapFromDto(academicOption, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getActive(), dto.getAcademicSectionId());
        return repository.save(academicOption);
    }

    public List<AcademicOption> findAll() {
        return repository.findAll();
    }

    public List<AcademicOption> findByAcademicSectionId(UUID academicSectionId) {
        return repository.findByAcademicSection_IdOrderByDisplayOrderAsc(academicSectionId);
    }

    public AcademicOption findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic option not found"));
    }

    public AcademicOption update(UUID id, UpdateAcademicOptionDto dto) {
        AcademicOption academicOption = findById(id);

        if (repository.existsByAcademicSection_IdAndCodeAndIdNot(
                dto.getAcademicSectionId(), dto.getCode(), id)) {
            throw new BadRequestException("Academic option code already exists for this section");
        }

        mapFromDto(academicOption, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getActive(), dto.getAcademicSectionId());
        return repository.save(academicOption);
    }

    public void delete(UUID id) {
        AcademicOption academicOption = findById(id);
        repository.delete(academicOption);
    }

    private void mapFromDto(
            AcademicOption academicOption,
            String code,
            String name,
            String description,
            Integer displayOrder,
            Boolean active,
            UUID academicSectionId
    ) {
        academicOption.setCode(code);
        academicOption.setName(name);
        academicOption.setDescription(description);
        academicOption.setDisplayOrder(displayOrder);
        academicOption.setActive(active != null ? active : true);
        academicOption.setAcademicSection(resolveAcademicSection(academicSectionId));
    }

    private AcademicSection resolveAcademicSection(UUID academicSectionId) {
        return academicSectionRepository.findById(academicSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic section not found"));
    }
}
