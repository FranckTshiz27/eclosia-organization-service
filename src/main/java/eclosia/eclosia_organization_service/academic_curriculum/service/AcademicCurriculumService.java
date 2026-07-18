package eclosia.eclosia_organization_service.academic_curriculum.service;

import eclosia.eclosia_organization_service.academic_curriculum.dto.CreateAcademicCurriculumDto;
import eclosia.eclosia_organization_service.academic_curriculum.dto.UpdateAcademicCurriculumDto;
import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import eclosia.eclosia_organization_service.academic_curriculum.repository.AcademicCurriculumRepository;
import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_level.repository.AcademicLevelRepository;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_option.repository.AcademicOptionRepository;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_section.repository.AcademicSectionRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicCurriculumService {

    private final AcademicCurriculumRepository repository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final AcademicSectionRepository academicSectionRepository;
    private final AcademicOptionRepository academicOptionRepository;

    public AcademicCurriculum create(CreateAcademicCurriculumDto dto) {
        assertUniqueCurriculum(
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                null
        );

        AcademicCurriculum academicCurriculum = new AcademicCurriculum();
        mapFromDto(
                academicCurriculum,
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getCode(),
                dto.getName(),
                dto.getActive()
        );
        return repository.save(academicCurriculum);
    }

    public List<AcademicCurriculum> findAll() {
        return repository.findAll();
    }

    public List<AcademicCurriculum> findByAcademicYearId(UUID academicYearId) {
        return repository.findByAcademicYear_Id(academicYearId);
    }

    public List<AcademicCurriculum> findByCountryId(UUID countryId) {
        return repository.findByAcademicYear_Country_Id(countryId);
    }

    public List<AcademicCurriculum> findByAcademicCycleId(UUID academicCycleId) {
        return repository.findByAcademicCycle_Id(academicCycleId);
    }

    public List<AcademicCurriculum> findByAcademicLevelId(UUID academicLevelId) {
        return repository.findByAcademicLevel_Id(academicLevelId);
    }

    public AcademicCurriculum findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic curriculum not found"));
    }

    public AcademicCurriculum update(UUID id, UpdateAcademicCurriculumDto dto) {
        AcademicCurriculum academicCurriculum = findById(id);

        assertUniqueCurriculum(
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                id
        );

        mapFromDto(
                academicCurriculum,
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getCode(),
                dto.getName(),
                dto.getActive()
        );
        return repository.save(academicCurriculum);
    }

    public void delete(UUID id) {
        AcademicCurriculum academicCurriculum = findById(id);
        repository.delete(academicCurriculum);
    }

    private void assertUniqueCurriculum(
            UUID academicYearId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID id
    ) {
        boolean exists = id == null
                ? repository.existsByCurriculumKeys(
                        academicYearId, academicCycleId, academicLevelId, academicSectionId, academicOptionId)
                : repository.existsByCurriculumKeysAndIdNot(
                        academicYearId, academicCycleId, academicLevelId, academicSectionId, academicOptionId, id);

        if (exists) {
            throw new BadRequestException("Academic curriculum already exists for this combination");
        }
    }

    private void mapFromDto(
            AcademicCurriculum academicCurriculum,
            UUID academicYearId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            String code,
            String name,
            Boolean active
    ) {
        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        AcademicCycle academicCycle = resolveAcademicCycle(academicCycleId);
        AcademicLevel academicLevel = resolveAcademicLevel(academicLevelId);
        AcademicSection academicSection = resolveAcademicSection(academicSectionId);
        AcademicOption academicOption = resolveAcademicOption(academicOptionId);

        validateAcademicStructure(
                academicCycle,
                academicLevel,
                academicSection,
                academicOption,
                academicSectionId,
                academicOptionId
        );

        academicCurriculum.setAcademicYear(academicYear);
        academicCurriculum.setAcademicCycle(academicCycle);
        academicCurriculum.setAcademicLevel(academicLevel);
        academicCurriculum.setAcademicSection(academicSection);
        academicCurriculum.setAcademicOption(academicOption);
        academicCurriculum.setCode(code);
        academicCurriculum.setName(name);
        academicCurriculum.setActive(active != null ? active : true);
    }

    private void validateAcademicStructure(
            AcademicCycle academicCycle,
            AcademicLevel academicLevel,
            AcademicSection academicSection,
            AcademicOption academicOption,
            UUID academicSectionId,
            UUID academicOptionId
    ) {
        if (!academicCycle.getId().equals(academicLevel.getAcademicCycle().getId())) {
            throw new BadRequestException("Academic level does not belong to the provided academic cycle");
        }

        validateLevelSectionAndOption(academicLevel, academicSectionId, academicOptionId);

        if (academicSection != null
                && !academicCycle.getId().equals(academicSection.getAcademicCycle().getId())) {
            throw new BadRequestException("Academic section does not belong to the provided academic cycle");
        }

        if (academicOptionId != null && academicSectionId == null) {
            throw new BusinessException("Une section est requise pour associer une option.");
        }

        if (academicOption != null
                && academicSection != null
                && !academicSection.getId().equals(academicOption.getAcademicSection().getId())) {
            throw new BadRequestException("Academic option does not belong to the provided academic section");
        }
    }

    private void validateLevelSectionAndOption(
            AcademicLevel level,
            UUID academicSectionId,
            UUID academicOptionId
    ) {
        if (Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId == null) {
            throw new BusinessException("Une section est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas de section.");
        }

        if (Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId == null) {
            throw new BusinessException("Une option est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas d'option.");
        }
    }

    private AcademicYear resolveAcademicYear(UUID academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private AcademicCycle resolveAcademicCycle(UUID academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic cycle not found"));
    }

    private AcademicLevel resolveAcademicLevel(UUID academicLevelId) {
        return academicLevelRepository.findById(academicLevelId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic level not found"));
    }

    private AcademicSection resolveAcademicSection(UUID academicSectionId) {
        if (academicSectionId == null) {
            return null;
        }
        return academicSectionRepository.findById(academicSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic section not found"));
    }

    private AcademicOption resolveAcademicOption(UUID academicOptionId) {
        if (academicOptionId == null) {
            return null;
        }
        return academicOptionRepository.findById(academicOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic option not found"));
    }
}
