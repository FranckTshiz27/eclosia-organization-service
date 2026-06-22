package eclosia.eclosia_organization_service.academic_model.controller;

import eclosia.eclosia_organization_service.academic_model.dto.CreateAcademicModelDto;
import eclosia.eclosia_organization_service.academic_model.dto.UpdateAcademicModelDto;
import eclosia.eclosia_organization_service.academic_model.entity.AcademicModel;
import eclosia.eclosia_organization_service.academic_model.service.AcademicModelService;
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
@RequestMapping(path = "academic-model")
@RequiredArgsConstructor
public class AcademicModelController {

    private final AcademicModelService service;

    @PostMapping
    public ResponseEntity<AcademicModel> create(@Valid @RequestBody CreateAcademicModelDto dto) {
        AcademicModel academicModel = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicModel);
    }

    @GetMapping
    public List<AcademicModel> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicModel findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicModel update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicModelDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
