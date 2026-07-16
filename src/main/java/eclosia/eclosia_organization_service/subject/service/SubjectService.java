package eclosia.eclosia_organization_service.subject.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import eclosia.eclosia_organization_service.subject.dto.CreateSubjectDto;
import eclosia.eclosia_organization_service.subject.dto.UpdateSubjectDto;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject.repository.SubjectRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class SubjectService {

    private final SubjectRepository repository;
    private final CountryRepository countryRepository;

    public Subject create(CreateSubjectDto dto) {
        if (repository.existsByCountry_IdAndCode(dto.getCountryId(), dto.getCode())) {
            throw new BadRequestException("Subject code already exists for this country");
        }

        Subject subject = new Subject();
        mapFromDto(
                subject,
                null,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getAbbreviation(),
                dto.getParentSubjectId(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subject);
    }

    public List<Subject> findAll() {
        return repository.findAll();
    }

    public List<Subject> findByCountryId(UUID countryId) {
        return repository.findByCountry_IdOrderByDisplayOrderAsc(countryId);
    }

    public List<Subject> findByParentSubjectId(UUID parentSubjectId) {
        return repository.findByParentSubject_IdOrderByDisplayOrderAsc(parentSubjectId);
    }

    public Subject findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    public Subject update(UUID id, UpdateSubjectDto dto) {
        Subject subject = findById(id);

        if (repository.existsByCountry_IdAndCodeAndIdNot(dto.getCountryId(), dto.getCode(), id)) {
            throw new BadRequestException("Subject code already exists for this country");
        }

        mapFromDto(
                subject,
                id,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getAbbreviation(),
                dto.getParentSubjectId(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subject);
    }

    public void delete(UUID id) {
        Subject subject = findById(id);
        repository.delete(subject);
    }

    private void mapFromDto(
            Subject subject,
            UUID subjectId,
            UUID countryId,
            String code,
            String name,
            String abbreviation,
            UUID parentSubjectId,
            Integer displayOrder,
            Boolean active
    ) {
        Country country = resolveCountry(countryId);
        subject.setCountry(country);
        subject.setCode(code);
        subject.setName(name);
        subject.setAbbreviation(abbreviation);
        subject.setParentSubject(resolveParentSubject(parentSubjectId, subjectId, countryId));
        subject.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        subject.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }

    private Subject resolveParentSubject(UUID parentSubjectId, UUID subjectId, UUID countryId) {
        if (parentSubjectId == null) {
            return null;
        }

        if (parentSubjectId.equals(subjectId)) {
            throw new BadRequestException("A subject cannot be its own parent");
        }

        Subject parentSubject = findById(parentSubjectId);

        if (!countryId.equals(parentSubject.getCountryId())) {
            throw new BadRequestException("Parent subject must belong to the same country");
        }

        return parentSubject;
    }
}
