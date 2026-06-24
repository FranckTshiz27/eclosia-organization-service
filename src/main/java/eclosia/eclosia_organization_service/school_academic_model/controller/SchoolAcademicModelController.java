package eclosia.eclosia_organization_service.school_academic_model.controller;

import eclosia.eclosia_organization_service.school_academic_model.dto.CreateSchoolAcademicModelDto;
import eclosia.eclosia_organization_service.school_academic_model.dto.UpdateSchoolAcademicModelDto;
import eclosia.eclosia_organization_service.school_academic_model.entity.SchoolAcademicModel;
import eclosia.eclosia_organization_service.school_academic_model.service.SchoolAcademicModelService;
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
@RequestMapping(path = "school-academic-model")
@RequiredArgsConstructor
public class SchoolAcademicModelController {

    private final SchoolAcademicModelService service;

    @PostMapping
    public ResponseEntity<SchoolAcademicModel> create(@Valid @RequestBody CreateSchoolAcademicModelDto dto) {
        SchoolAcademicModel schoolAcademicModel = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolAcademicModel);
    }

    @GetMapping
    public List<SchoolAcademicModel> findAll(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID academicModelId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        if (academicModelId != null) {
            return service.findByAcademicModelId(academicModelId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SchoolAcademicModel findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public SchoolAcademicModel update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolAcademicModelDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
