package eclosia.eclosia_organization_service.academic_curriculum.controller;

import eclosia.eclosia_organization_service.academic_curriculum.dto.CreateAcademicCurriculumDto;
import eclosia.eclosia_organization_service.academic_curriculum.dto.UpdateAcademicCurriculumDto;
import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import eclosia.eclosia_organization_service.academic_curriculum.service.AcademicCurriculumService;
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
@RequestMapping(path = "academic-curriculum")
@RequiredArgsConstructor
public class AcademicCurriculumController {

    private final AcademicCurriculumService service;

    @PostMapping
    public ResponseEntity<AcademicCurriculum> create(@Valid @RequestBody CreateAcademicCurriculumDto dto) {
        AcademicCurriculum academicCurriculum = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicCurriculum);
    }

    @GetMapping
    public List<AcademicCurriculum> findAll(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) UUID academicCycleId,
            @RequestParam(required = false) UUID academicLevelId
    ) {
        if (academicLevelId != null) {
            return service.findByAcademicLevelId(academicLevelId);
        }
        if (academicCycleId != null) {
            return service.findByAcademicCycleId(academicCycleId);
        }
        if (academicYearId != null) {
            return service.findByAcademicYearId(academicYearId);
        }
        if (countryId != null) {
            return service.findByCountryId(countryId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicCurriculum findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicCurriculum update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicCurriculumDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
