package eclosia.eclosia_organization_service.academic_year.controller;

import eclosia.eclosia_organization_service.academic_year.dto.CreateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.dto.UpdateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.service.AcademicYearService;
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
@RequestMapping(path = "academic-year")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService service;

    @PostMapping
    public ResponseEntity<AcademicYear> create(@Valid @RequestBody CreateAcademicYearDto dto) {
        AcademicYear academicYear = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicYear);
    }

    @GetMapping
    public List<AcademicYear> findAll(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID schoolAcademicModelId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        if (schoolAcademicModelId != null) {
            return service.findBySchoolAcademicModelId(schoolAcademicModelId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicYear findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicYear update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicYearDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
