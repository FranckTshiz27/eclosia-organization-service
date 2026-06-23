package eclosia.eclosia_organization_service.academic_section.controller;

import eclosia.eclosia_organization_service.academic_section.dto.CreateAcademicSectionDto;
import eclosia.eclosia_organization_service.academic_section.dto.UpdateAcademicSectionDto;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_section.service.AcademicSectionService;
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
@RequestMapping(path = "academic-section")
@RequiredArgsConstructor
public class AcademicSectionController {

    private final AcademicSectionService service;

    @PostMapping
    public ResponseEntity<AcademicSection> create(@Valid @RequestBody CreateAcademicSectionDto dto) {
        AcademicSection academicSection = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicSection);
    }

    @GetMapping
    public List<AcademicSection> findAll(
            @RequestParam(required = false) UUID academicCycleId
    ) {
        if (academicCycleId != null) {
            return service.findByAcademicCycleId(academicCycleId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicSection findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicSection update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicSectionDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
