package eclosia.eclosia_organization_service.academic_cycle.service;

import eclosia.eclosia_organization_service.academic_cycle.dto.CreateAcademicCycleDto;
import eclosia.eclosia_organization_service.academic_cycle.dto.UpdateAcademicCycleDto;
import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_model.entity.AcademicModel;
import eclosia.eclosia_organization_service.academic_model.repository.AcademicModelRepository;
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
public class AcademicCycleService {

    private final AcademicCycleRepository repository;
    private final AcademicModelRepository academicModelRepository;

    public AcademicCycle create(CreateAcademicCycleDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Academic cycle code already exists");
        }

        AcademicCycle academicCycle = new AcademicCycle();
        mapFromDto(academicCycle, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getDurationYears(), dto.getActive(), dto.getAcademicModelId());
        return repository.save(academicCycle);
    }

    public List<AcademicCycle> findAll() {
        return repository.findAll();
    }

    public AcademicCycle findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic cycle not found"));
    }

    public AcademicCycle update(UUID id, UpdateAcademicCycleDto dto) {
        AcademicCycle academicCycle = findById(id);

        if (repository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new BadRequestException("Academic cycle code already exists");
        }

        mapFromDto(academicCycle, dto.getCode(), dto.getName(), dto.getDescription(),
                dto.getDisplayOrder(), dto.getDurationYears(), dto.getActive(), dto.getAcademicModelId());
        return repository.save(academicCycle);
    }

    public void delete(UUID id) {
        AcademicCycle academicCycle = findById(id);
        repository.delete(academicCycle);
    }

    private void mapFromDto(
            AcademicCycle academicCycle,
            String code,
            String name,
            String description,
            Integer displayOrder,
            Integer durationYears,
            Boolean active,
            UUID academicModelId
    ) {
        academicCycle.setCode(code);
        academicCycle.setName(name);
        academicCycle.setDescription(description);
        academicCycle.setDisplayOrder(displayOrder);
        academicCycle.setDurationYears(durationYears);
        academicCycle.setActive(active != null ? active : true);
        academicCycle.setAcademicModel(resolveAcademicModel(academicModelId));
    }

    private AcademicModel resolveAcademicModel(UUID academicModelId) {
        return academicModelRepository.findById(academicModelId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic model not found"));
    }
}
