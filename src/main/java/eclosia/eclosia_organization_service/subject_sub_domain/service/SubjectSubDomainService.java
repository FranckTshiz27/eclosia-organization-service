package eclosia.eclosia_organization_service.subject_sub_domain.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import eclosia.eclosia_organization_service.subject_domain.repository.SubjectDomainRepository;
import eclosia.eclosia_organization_service.subject_sub_domain.dto.CreateSubjectSubDomainDto;
import eclosia.eclosia_organization_service.subject_sub_domain.dto.UpdateSubjectSubDomainDto;
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
public class SubjectSubDomainService {

    private final SubjectSubDomainRepository repository;
    private final SubjectDomainRepository subjectDomainRepository;

    public SubjectSubDomain create(CreateSubjectSubDomainDto dto) {
        if (repository.existsBySubjectDomain_IdAndCode(dto.getSubjectDomainId(), dto.getCode())) {
            throw new BadRequestException("Subject sub domain code already exists for this subject domain");
        }

        SubjectSubDomain subjectSubDomain = new SubjectSubDomain();
        mapFromDto(
                subjectSubDomain,
                dto.getSubjectDomainId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subjectSubDomain);
    }

    public List<SubjectSubDomain> findAll() {
        return repository.findAll();
    }

    public List<SubjectSubDomain> findBySubjectDomainId(UUID subjectDomainId) {
        return repository.findBySubjectDomain_IdOrderByDisplayOrderAsc(subjectDomainId);
    }

    public SubjectSubDomain findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject sub domain not found"));
    }

    public SubjectSubDomain update(UUID id, UpdateSubjectSubDomainDto dto) {
        SubjectSubDomain subjectSubDomain = findById(id);

        if (repository.existsBySubjectDomain_IdAndCodeAndIdNot(dto.getSubjectDomainId(), dto.getCode(), id)) {
            throw new BadRequestException("Subject sub domain code already exists for this subject domain");
        }

        mapFromDto(
                subjectSubDomain,
                dto.getSubjectDomainId(),
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getActive()
        );
        return repository.save(subjectSubDomain);
    }

    public void delete(UUID id) {
        SubjectSubDomain subjectSubDomain = findById(id);
        repository.delete(subjectSubDomain);
    }

    private void mapFromDto(
            SubjectSubDomain subjectSubDomain,
            UUID subjectDomainId,
            String code,
            String name,
            Integer displayOrder,
            Boolean active
    ) {
        subjectSubDomain.setSubjectDomain(resolveSubjectDomain(subjectDomainId));
        subjectSubDomain.setCode(code);
        subjectSubDomain.setName(name);
        subjectSubDomain.setDisplayOrder(displayOrder != null ? displayOrder : 1);
        subjectSubDomain.setActive(active != null ? active : true);
    }

    private SubjectDomain resolveSubjectDomain(UUID subjectDomainId) {
        return subjectDomainRepository.findById(subjectDomainId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject domain not found"));
    }
}
