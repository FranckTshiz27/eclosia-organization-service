package eclosia.eclosia_organization_service.academic_cycle.controller;

import eclosia.eclosia_organization_service.academic_cycle.dto.CreateAcademicCycleDto;
import eclosia.eclosia_organization_service.academic_cycle.dto.UpdateAcademicCycleDto;
import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.service.AcademicCycleService;
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
@RequestMapping(path = "academic-cycle")
@RequiredArgsConstructor
public class AcademicCycleController {

    private final AcademicCycleService service;

    @PostMapping
    public ResponseEntity<AcademicCycle> create(@Valid @RequestBody CreateAcademicCycleDto dto) {
        AcademicCycle academicCycle = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicCycle);
    }

    @GetMapping
    public List<AcademicCycle> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicCycle findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicCycle update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicCycleDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
