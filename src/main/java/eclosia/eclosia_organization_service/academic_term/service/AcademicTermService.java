package eclosia.eclosia_organization_service.academic_term.service;

import eclosia.eclosia_organization_service.academic_term.dto.CreateAcademicTermDto;
import eclosia.eclosia_organization_service.academic_term.dto.UpdateAcademicTermDto;
import eclosia.eclosia_organization_service.academic_term.entity.AcademicTerm;
import eclosia.eclosia_organization_service.academic_term.repository.AcademicTermRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
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
public class AcademicTermService {

    private final AcademicTermRepository repository;
    private final AcademicYearRepository academicYearRepository;

    public AcademicTerm create(CreateAcademicTermDto dto) {
        if (repository.existsByAcademicYear_IdAndCode(dto.getAcademicYearId(), dto.getCode())) {
            throw new BadRequestException("Academic term code already exists for this academic year");
        }

        AcademicTerm academicTerm = new AcademicTerm();
        mapFromDto(
                academicTerm,
                dto.getAcademicYearId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(academicTerm);
    }

    public List<AcademicTerm> findAll() {
        return repository.findAll();
    }

    public List<AcademicTerm> findByAcademicYearId(UUID academicYearId) {
        return repository.findByAcademicYear_IdOrderByDisplayOrderAsc(academicYearId);
    }

    public AcademicTerm findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    public AcademicTerm update(UUID id, UpdateAcademicTermDto dto) {
        AcademicTerm academicTerm = findById(id);

        if (repository.existsByAcademicYear_IdAndCodeAndIdNot(
                dto.getAcademicYearId(),
                dto.getCode(),
                id
        )) {
            throw new BadRequestException("Academic term code already exists for this academic year");
        }

        mapFromDto(
                academicTerm,
                dto.getAcademicYearId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(academicTerm);
    }

    public void delete(UUID id) {
        AcademicTerm academicTerm = findById(id);
        repository.delete(academicTerm);
    }

    private void mapFromDto(
            AcademicTerm academicTerm,
            UUID academicYearId,
            String code,
            String name,
            Integer displayOrder,
            Boolean active
    ) {
        academicTerm.setAcademicYear(resolveAcademicYear(academicYearId));
        academicTerm.setCode(code);
        academicTerm.setName(name);
        academicTerm.setDisplayOrder(displayOrder);
        academicTerm.setActive(active != null ? active : true);
    }

    private AcademicYear resolveAcademicYear(UUID academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }
}
