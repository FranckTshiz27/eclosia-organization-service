package eclosia.eclosia_organization_service.subject.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import eclosia.eclosia_organization_service.subject.dto.CreateSubjectDto;
import eclosia.eclosia_organization_service.subject.dto.UpdateSubjectDto;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject.repository.SubjectRepository;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import eclosia.eclosia_organization_service.subject_domain.repository.SubjectDomainRepository;
import eclosia.eclosia_organization_service.subject_sub_domain.entity.SubjectSubDomain;
import eclosia.eclosia_organization_service.subject_sub_domain.repository.SubjectSubDomainRepository;
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
    private final SubjectDomainRepository subjectDomainRepository;
    private final SubjectSubDomainRepository subjectSubDomainRepository;

    public Subject create(CreateSubjectDto dto) {
        if (repository.existsByCountry_IdAndCode(dto.getCountryId(), dto.getCode())) {
            throw new BadRequestException("Subject code already exists for this country");
        }

        Subject subject = new Subject();
        mapFromDto(
                subject,
                dto.getCountryId(),
                dto.getSubjectDomainId(),
                dto.getSubjectSubDomainId(),
                dto.getCode(),
                dto.getName(),
                dto.getAbbreviation(),
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

    public List<Subject> findBySubjectDomainId(UUID subjectDomainId) {
        return repository.findBySubjectDomain_IdOrderByDisplayOrderAsc(subjectDomainId);
    }

    public List<Subject> findBySubjectSubDomainId(UUID subjectSubDomainId) {
        return repository.findBySubjectSubDomain_IdOrderByDisplayOrderAsc(subjectSubDomainId);
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
                dto.getCountryId(),
                dto.getSubjectDomainId(),
                dto.getSubjectSubDomainId(),
                dto.getCode(),
                dto.getName(),
                dto.getAbbreviation(),
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
            UUID countryId,
            UUID subjectDomainId,
            UUID subjectSubDomainId,
            String code,
            String name,
            String abbreviation,
            Integer displayOrder,
            Boolean active
    ) {
        Country country = resolveCountry(countryId);
        SubjectDomain subjectDomain = resolveSubjectDomain(subjectDomainId, countryId);
        SubjectSubDomain subjectSubDomain = resolveSubjectSubDomain(
                subjectSubDomainId,
                subjectDomainId,
                subjectDomain
        );

        subject.setCountry(country);
        subject.setSubjectDomain(subjectDomain);
        subject.setSubjectSubDomain(subjectSubDomain);
        subject.setCode(code);
        subject.setName(name);
        subject.setAbbreviation(abbreviation);
        subject.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        subject.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }

    private SubjectDomain resolveSubjectDomain(UUID subjectDomainId, UUID countryId) {
        if (subjectDomainId == null) {
            return null;
        }

        SubjectDomain subjectDomain = subjectDomainRepository.findById(subjectDomainId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject domain not found"));

        if (!countryId.equals(subjectDomain.getCountryId())) {
            throw new BadRequestException("Subject domain must belong to the same country");
        }

        return subjectDomain;
    }

    private SubjectSubDomain resolveSubjectSubDomain(
            UUID subjectSubDomainId,
            UUID subjectDomainId,
            SubjectDomain subjectDomain
    ) {
        if (subjectSubDomainId == null) {
            return null;
        }

        if (subjectDomain == null) {
            throw new BadRequestException("Subject domain is required when subject sub domain is provided");
        }

        SubjectSubDomain subjectSubDomain = subjectSubDomainRepository.findById(subjectSubDomainId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject sub domain not found"));

        if (!subjectDomainId.equals(subjectSubDomain.getSubjectDomainId())) {
            throw new BadRequestException("Subject sub domain must belong to the selected subject domain");
        }

        return subjectSubDomain;
    }
}
