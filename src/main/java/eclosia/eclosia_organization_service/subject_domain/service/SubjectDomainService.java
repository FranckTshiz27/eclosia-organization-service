package eclosia.eclosia_organization_service.subject_domain.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import eclosia.eclosia_organization_service.subject_domain.dto.CreateSubjectDomainDto;
import eclosia.eclosia_organization_service.subject_domain.dto.UpdateSubjectDomainDto;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import eclosia.eclosia_organization_service.subject_domain.repository.SubjectDomainRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class SubjectDomainService {

    private final SubjectDomainRepository repository;
    private final CountryRepository countryRepository;

    public SubjectDomain create(CreateSubjectDomainDto dto) {
        if (repository.existsByCountry_IdAndCode(dto.getCountryId(), dto.getCode())) {
            throw new BadRequestException("Subject domain code already exists for this country");
        }

        SubjectDomain subjectDomain = new SubjectDomain();
        mapFromDto(
                subjectDomain,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subjectDomain);
    }

    public List<SubjectDomain> findAll() {
        return repository.findAll();
    }

    public List<SubjectDomain> findByCountryId(UUID countryId) {
        return repository.findByCountry_IdOrderByDisplayOrderAsc(countryId);
    }

    public SubjectDomain findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject domain not found"));
    }

    public SubjectDomain update(UUID id, UpdateSubjectDomainDto dto) {
        SubjectDomain subjectDomain = findById(id);

        if (repository.existsByCountry_IdAndCodeAndIdNot(dto.getCountryId(), dto.getCode(), id)) {
            throw new BadRequestException("Subject domain code already exists for this country");
        }

        mapFromDto(
                subjectDomain,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subjectDomain);
    }

    public void delete(UUID id) {
        SubjectDomain subjectDomain = findById(id);
        repository.delete(subjectDomain);
    }

    private void mapFromDto(
            SubjectDomain subjectDomain,
            UUID countryId,
            String code,
            String name,
            Integer displayOrder,
            Boolean active
    ) {
        subjectDomain.setCountry(resolveCountry(countryId));
        subjectDomain.setCode(code);
        subjectDomain.setName(name);
        subjectDomain.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        subjectDomain.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }
}
