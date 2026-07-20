package eclosia.eclosia_organization_service.academic_term.controller;

import eclosia.eclosia_organization_service.academic_term.dto.CreateAcademicTermDto;
import eclosia.eclosia_organization_service.academic_term.dto.UpdateAcademicTermDto;
import eclosia.eclosia_organization_service.academic_term.entity.AcademicTerm;
import eclosia.eclosia_organization_service.academic_term.service.AcademicTermService;
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
@RequestMapping(path = "academic-term")
@RequiredArgsConstructor
public class AcademicTermController {

    private final AcademicTermService service;

    @PostMapping
    public ResponseEntity<AcademicTerm> create(@Valid @RequestBody CreateAcademicTermDto dto) {
        AcademicTerm academicTerm = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicTerm);
    }

    @GetMapping
    public List<AcademicTerm> findAll(@RequestParam(required = false) UUID academicYearId) {
        if (academicYearId != null) {
            return service.findByAcademicYearId(academicYearId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicTerm findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicTerm update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicTermDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
