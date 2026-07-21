package eclosia.eclosia_organization_service.academic_period.service;

import eclosia.eclosia_organization_service.academic_period.dto.CreateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.dto.UpdateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.academic_period.enums.AcademicPeriodType;
import eclosia.eclosia_organization_service.academic_period.repository.AcademicPeriodRepository;
import eclosia.eclosia_organization_service.academic_term.entity.AcademicTerm;
import eclosia.eclosia_organization_service.academic_term.repository.AcademicTermRepository;
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
public class AcademicPeriodService {

    private final AcademicPeriodRepository repository;
    private final AcademicTermRepository academicTermRepository;

    public AcademicPeriod create(CreateAcademicPeriodDto dto) {
        if (repository.existsByAcademicTerm_IdAndCode(dto.getAcademicTermId(), dto.getCode())) {
            throw new BadRequestException("Academic period code already exists for this academic term");
        }

        AcademicPeriod academicPeriod = new AcademicPeriod();
        mapFromDto(
                academicPeriod,
                dto.getAcademicTermId(),
                dto.getCode(),
                dto.getName(),
                dto.getPeriodType(),
                dto.getDisplayOrder(),
                dto.getMaximumScoreRatio(),
                dto.getActive()
        );
        return repository.save(academicPeriod);
    }

    public List<AcademicPeriod> findAll() {
        return repository.findAll();
    }

    public List<AcademicPeriod> findByAcademicTermId(UUID academicTermId) {
        return repository.findByAcademicTerm_IdOrderByDisplayOrderAsc(academicTermId);
    }

    public AcademicPeriod findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found"));
    }

    public AcademicPeriod update(UUID id, UpdateAcademicPeriodDto dto) {
        AcademicPeriod academicPeriod = findById(id);

        if (repository.existsByAcademicTerm_IdAndCodeAndIdNot(
                dto.getAcademicTermId(),
                dto.getCode(),
                id
        )) {
            throw new BadRequestException("Academic period code already exists for this academic term");
        }

        mapFromDto(
                academicPeriod,
                dto.getAcademicTermId(),
                dto.getCode(),
                dto.getName(),
                dto.getPeriodType(),
                dto.getDisplayOrder(),
                dto.getMaximumScoreRatio(),
                dto.getActive()
        );
        return repository.save(academicPeriod);
    }

    public void delete(UUID id) {
        AcademicPeriod academicPeriod = findById(id);
        repository.delete(academicPeriod);
    }

    private void mapFromDto(
            AcademicPeriod academicPeriod,
            UUID academicTermId,
            String code,
            String name,
            AcademicPeriodType periodType,
            Integer displayOrder,
            Integer maximumScoreRatio,
            Boolean active
    ) {
        academicPeriod.setAcademicTerm(resolveAcademicTerm(academicTermId));
        academicPeriod.setCode(code);
        academicPeriod.setName(name);
        academicPeriod.setPeriodType(periodType);
        academicPeriod.setDisplayOrder(displayOrder);
        academicPeriod.setMaximumScoreRatio(maximumScoreRatio);
        academicPeriod.setActive(active != null ? active : true);
    }

    private AcademicTerm resolveAcademicTerm(UUID academicTermId) {
        return academicTermRepository.findById(academicTermId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }
}
