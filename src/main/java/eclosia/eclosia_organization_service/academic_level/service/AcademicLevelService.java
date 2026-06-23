package eclosia.eclosia_organization_service.academic_level.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_level.dto.CreateAcademicLevelDto;
import eclosia.eclosia_organization_service.academic_level.dto.UpdateAcademicLevelDto;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_level.repository.AcademicLevelRepository;
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
public class AcademicLevelService {

    private final AcademicLevelRepository repository;
    private final AcademicCycleRepository academicCycleRepository;

    public AcademicLevel create(CreateAcademicLevelDto dto) {
        if (repository.existsByAcademicCycle_IdAndCode(dto.getAcademicCycleId(), dto.getCode())) {
            throw new BadRequestException("Academic level code already exists for this cycle");
        }

        AcademicLevel academicLevel = new AcademicLevel();
        mapFromDto(academicLevel, dto.getCode(), dto.getName(), dto.getLevelOrder(),
                dto.getRequiresSection(), dto.getRequiresOption(), dto.getActive(), dto.getAcademicCycleId());
        return repository.save(academicLevel);
    }

    public List<AcademicLevel> findAll() {
        return repository.findAll();
    }

    public AcademicLevel findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic level not found"));
    }

    public AcademicLevel update(UUID id, UpdateAcademicLevelDto dto) {
        AcademicLevel academicLevel = findById(id);

        if (repository.existsByAcademicCycle_IdAndCodeAndIdNot(
                dto.getAcademicCycleId(), dto.getCode(), id)) {
            throw new BadRequestException("Academic level code already exists for this cycle");
        }

        mapFromDto(academicLevel, dto.getCode(), dto.getName(), dto.getLevelOrder(),
                dto.getRequiresSection(), dto.getRequiresOption(), dto.getActive(), dto.getAcademicCycleId());
        return repository.save(academicLevel);
    }

    public void delete(UUID id) {
        AcademicLevel academicLevel = findById(id);
        repository.delete(academicLevel);
    }

    private void mapFromDto(
            AcademicLevel academicLevel,
            String code,
            String name,
            Integer levelOrder,
            Boolean requiresSection,
            Boolean requiresOption,
            Boolean active,
            UUID academicCycleId
    ) {
        academicLevel.setCode(code);
        academicLevel.setName(name);
        academicLevel.setLevelOrder(levelOrder);
        academicLevel.setRequiresSection(requiresSection != null ? requiresSection : false);
        academicLevel.setRequiresOption(requiresOption != null ? requiresOption : false);
        academicLevel.setActive(active != null ? active : true);
        academicLevel.setAcademicCycle(resolveAcademicCycle(academicCycleId));
    }

    private AcademicCycle resolveAcademicCycle(UUID academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic cycle not found"));
    }
}
