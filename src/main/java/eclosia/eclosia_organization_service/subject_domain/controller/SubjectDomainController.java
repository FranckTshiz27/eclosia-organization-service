package eclosia.eclosia_organization_service.subject_domain.controller;

import eclosia.eclosia_organization_service.subject_domain.dto.CreateSubjectDomainDto;
import eclosia.eclosia_organization_service.subject_domain.dto.UpdateSubjectDomainDto;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import eclosia.eclosia_organization_service.subject_domain.service.SubjectDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "subject-domain")
@RequiredArgsConstructor
public class SubjectDomainController {

    private final SubjectDomainService service;

    @PostMapping
    public ResponseEntity<SubjectDomain> create(@Valid @RequestBody CreateSubjectDomainDto dto) {
        SubjectDomain subjectDomain = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectDomain);
    }

    @GetMapping
    public List<SubjectDomain> findAll(@RequestParam(required = false) UUID countryId) {
        if (countryId != null) {
            return service.findByCountryId(countryId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SubjectDomain findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public SubjectDomain update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectDomainDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
