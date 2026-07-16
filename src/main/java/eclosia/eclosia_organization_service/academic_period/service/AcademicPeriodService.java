package eclosia.eclosia_organization_service.academic_period.service;

import eclosia.eclosia_organization_service.academic_period.dto.CreateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.dto.UpdateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.academic_period.repository.AcademicPeriodRepository;
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
public class AcademicPeriodService {

    private final AcademicPeriodRepository repository;
    private final CountryRepository countryRepository;

    public AcademicPeriod create(CreateAcademicPeriodDto dto) {
        if (repository.existsByCountry_IdAndCode(dto.getCountryId(), dto.getCode())) {
            throw new BadRequestException("Academic period code already exists for this country");
        }

        AcademicPeriod academicPeriod = new AcademicPeriod();
        mapFromDto(
                academicPeriod,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getOrderNumber(),
                dto.getActive()
        );
        return repository.save(academicPeriod);
    }

    public List<AcademicPeriod> findAll() {
        return repository.findAll();
    }

    public List<AcademicPeriod> findByCountryId(UUID countryId) {
        return repository.findByCountry_IdOrderByOrderNumberAsc(countryId);
    }

    public AcademicPeriod findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found"));
    }

    public AcademicPeriod update(UUID id, UpdateAcademicPeriodDto dto) {
        AcademicPeriod academicPeriod = findById(id);

        if (repository.existsByCountry_IdAndCodeAndIdNot(dto.getCountryId(), dto.getCode(), id)) {
            throw new BadRequestException("Academic period code already exists for this country");
        }

        mapFromDto(
                academicPeriod,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getOrderNumber(),
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
            UUID countryId,
            String code,
            String name,
            Integer orderNumber,
            Boolean active
    ) {
        academicPeriod.setCountry(resolveCountry(countryId));
        academicPeriod.setCode(code);
        academicPeriod.setName(name);
        academicPeriod.setOrderNumber(orderNumber);
        academicPeriod.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }
}
