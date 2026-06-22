package eclosia.eclosia_organization_service.academic_model.service;

import eclosia.eclosia_organization_service.academic_model.dto.CreateAcademicModelDto;
import eclosia.eclosia_organization_service.academic_model.dto.UpdateAcademicModelDto;
import eclosia.eclosia_organization_service.academic_model.entity.AcademicModel;
import eclosia.eclosia_organization_service.academic_model.repository.AcademicModelRepository;
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
public class AcademicModelService {

    private final AcademicModelRepository repository;
    private final CountryRepository countryRepository;

    public AcademicModel create(CreateAcademicModelDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Academic model code already exists");
        }

        AcademicModel academicModel = new AcademicModel();
        academicModel.setCode(dto.getCode());
        academicModel.setName(dto.getName());
        academicModel.setVersion(dto.getVersion());
        academicModel.setStartYear(dto.getStartYear());
        academicModel.setEndYear(dto.getEndYear());
        academicModel.setActive(dto.getActive() != null ? dto.getActive() : true);
        academicModel.setCountry(resolveCountry(dto.getCountryId()));
        return repository.save(academicModel);
    }

    public List<AcademicModel> findAll() {
        return repository.findAll();
    }

    public AcademicModel findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic model not found"));
    }

    public AcademicModel update(UUID id, UpdateAcademicModelDto dto) {
        AcademicModel academicModel = findById(id);

        if (repository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new BadRequestException("Academic model code already exists");
        }

        academicModel.setCode(dto.getCode());
        academicModel.setName(dto.getName());
        academicModel.setVersion(dto.getVersion());
        academicModel.setStartYear(dto.getStartYear());
        academicModel.setEndYear(dto.getEndYear());
        academicModel.setActive(dto.getActive() != null ? dto.getActive() : true);
        academicModel.setCountry(resolveCountry(dto.getCountryId()));
        return repository.save(academicModel);
    }

    public void delete(UUID id) {
        AcademicModel academicModel = findById(id);
        repository.delete(academicModel);
    }

    private Country resolveCountry(UUID countryId) {
        if (countryId == null) {
            return null;
        }

        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }
}
