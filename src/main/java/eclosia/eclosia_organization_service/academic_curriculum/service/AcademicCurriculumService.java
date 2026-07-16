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
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
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
    private final CountryRepository countryRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final AcademicSectionRepository academicSectionRepository;
    private final AcademicOptionRepository academicOptionRepository;

    public AcademicCurriculum create(CreateAcademicCurriculumDto dto) {
        assertUniqueCurriculum(
                dto.getCountryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                null
        );

        AcademicCurriculum academicCurriculum = new AcademicCurriculum();
        mapFromDto(
                academicCurriculum,
                dto.getCountryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getActive()
        );
        return repository.save(academicCurriculum);
    }

    public List<AcademicCurriculum> findAll() {
        return repository.findAll();
    }

    public List<AcademicCurriculum> findByCountryId(UUID countryId) {
        return repository.findByCountry_Id(countryId);
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
                dto.getCountryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                id
        );

        mapFromDto(
                academicCurriculum,
                dto.getCountryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getActive()
        );
        return repository.save(academicCurriculum);
    }

    public void delete(UUID id) {
        AcademicCurriculum academicCurriculum = findById(id);
        repository.delete(academicCurriculum);
    }

    private void assertUniqueCurriculum(
            UUID countryId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID id
    ) {
        boolean exists = id == null
                ? repository.existsByCurriculumKeys(
                        countryId, academicCycleId, academicLevelId, academicSectionId, academicOptionId)
                : repository.existsByCurriculumKeysAndIdNot(
                        countryId, academicCycleId, academicLevelId, academicSectionId, academicOptionId, id);

        if (exists) {
            throw new BadRequestException("Academic curriculum already exists for this combination");
        }
    }

    private void mapFromDto(
            AcademicCurriculum academicCurriculum,
            UUID countryId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            Boolean active
    ) {
        academicCurriculum.setCountry(resolveCountry(countryId));
        academicCurriculum.setAcademicCycle(resolveAcademicCycle(academicCycleId));
        academicCurriculum.setAcademicLevel(resolveAcademicLevel(academicLevelId));
        academicCurriculum.setAcademicSection(resolveAcademicSection(academicSectionId));
        academicCurriculum.setAcademicOption(resolveAcademicOption(academicOptionId));
        academicCurriculum.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
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
