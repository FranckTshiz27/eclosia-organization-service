package eclosia.eclosia_organization_service.subject_sub_domain.controller;

import eclosia.eclosia_organization_service.subject_sub_domain.dto.CreateSubjectSubDomainDto;
import eclosia.eclosia_organization_service.subject_sub_domain.dto.UpdateSubjectSubDomainDto;
import eclosia.eclosia_organization_service.subject_sub_domain.entity.SubjectSubDomain;
import eclosia.eclosia_organization_service.subject_sub_domain.service.SubjectSubDomainService;
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
@RequestMapping(path = "subject-sub-domain")
@RequiredArgsConstructor
public class SubjectSubDomainController {

    private final SubjectSubDomainService service;

    @PostMapping
    public ResponseEntity<SubjectSubDomain> create(@Valid @RequestBody CreateSubjectSubDomainDto dto) {
        SubjectSubDomain subjectSubDomain = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectSubDomain);
    }

    @GetMapping
    public List<SubjectSubDomain> findAll(@RequestParam(required = false) UUID subjectDomainId) {
        if (subjectDomainId != null) {
            return service.findBySubjectDomainId(subjectDomainId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SubjectSubDomain findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public SubjectSubDomain update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectSubDomainDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
