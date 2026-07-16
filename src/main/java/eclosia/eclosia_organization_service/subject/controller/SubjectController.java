package eclosia.eclosia_organization_service.subject.controller;

import eclosia.eclosia_organization_service.subject.dto.CreateSubjectDto;
import eclosia.eclosia_organization_service.subject.dto.UpdateSubjectDto;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject.service.SubjectService;
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
@RequestMapping(path = "subject")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService service;

    @PostMapping
    public ResponseEntity<Subject> create(@Valid @RequestBody CreateSubjectDto dto) {
        Subject subject = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(subject);
    }

    @GetMapping
    public List<Subject> findAll(
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) UUID parentSubjectId
    ) {
        if (parentSubjectId != null) {
            return service.findByParentSubjectId(parentSubjectId);
        }
        if (countryId != null) {
            return service.findByCountryId(countryId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Subject findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Subject update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
