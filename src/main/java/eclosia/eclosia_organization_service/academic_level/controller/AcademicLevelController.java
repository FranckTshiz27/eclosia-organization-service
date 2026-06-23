package eclosia.eclosia_organization_service.academic_level.controller;

import eclosia.eclosia_organization_service.academic_level.dto.CreateAcademicLevelDto;
import eclosia.eclosia_organization_service.academic_level.dto.UpdateAcademicLevelDto;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_level.service.AcademicLevelService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "academic-level")
@RequiredArgsConstructor
public class AcademicLevelController {

    private final AcademicLevelService service;

    @PostMapping
    public ResponseEntity<AcademicLevel> create(@Valid @RequestBody CreateAcademicLevelDto dto) {
        AcademicLevel academicLevel = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicLevel);
    }

    @GetMapping
    public List<AcademicLevel> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicLevel findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicLevel update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicLevelDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
