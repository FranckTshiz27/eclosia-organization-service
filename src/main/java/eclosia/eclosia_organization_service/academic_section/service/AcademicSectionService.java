package eclosia.eclosia_organization_service.academic_section.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_section.dto.CreateAcademicSectionDto;
import eclosia.eclosia_organization_service.academic_section.dto.UpdateAcademicSectionDto;
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
public class AcademicSectionService {

    private final AcademicSectionRepository repository;
    private final AcademicCycleRepository academicCycleRepository;

    public AcademicSection create(CreateAcademicSectionDto dto) {
        if (repository.existsByAcademicCycle_IdAndCode(dto.getAcademicCycleId(), dto.getCode())) {
            throw new BadRequestException("Academic section code already exists for this cycle");
        }

        AcademicSection academicSection = new AcademicSection();
        mapFromDto(academicSection, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getActive(), dto.getAcademicCycleId());
        return repository.save(academicSection);
    }

    public List<AcademicSection> findAll() {
        return repository.findAll();
    }

    public List<AcademicSection> findByAcademicCycleId(UUID academicCycleId) {
        return repository.findByAcademicCycle_IdOrderByDisplayOrderAsc(academicCycleId);
    }

    public AcademicSection findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic section not found"));
    }

    public AcademicSection update(UUID id, UpdateAcademicSectionDto dto) {
        AcademicSection academicSection = findById(id);

        if (repository.existsByAcademicCycle_IdAndCodeAndIdNot(
                dto.getAcademicCycleId(), dto.getCode(), id)) {
            throw new BadRequestException("Academic section code already exists for this cycle");
        }

        mapFromDto(academicSection, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getActive(), dto.getAcademicCycleId());
        return repository.save(academicSection);
    }

    public void delete(UUID id) {
        AcademicSection academicSection = findById(id);
        repository.delete(academicSection);
    }

    private void mapFromDto(
            AcademicSection academicSection,
            String code,
            String name,
            String description,
            Integer displayOrder,
            Boolean active,
            UUID academicCycleId
    ) {
        academicSection.setCode(code);
        academicSection.setName(name);
        academicSection.setDescription(description);
        academicSection.setDisplayOrder(displayOrder);
        academicSection.setActive(active != null ? active : true);
        academicSection.setAcademicCycle(resolveAcademicCycle(academicCycleId));
    }

    private AcademicCycle resolveAcademicCycle(UUID academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic cycle not found"));
    }
}
