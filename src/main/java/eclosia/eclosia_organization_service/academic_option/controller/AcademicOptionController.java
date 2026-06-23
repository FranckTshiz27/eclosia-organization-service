package eclosia.eclosia_organization_service.academic_option.controller;

import eclosia.eclosia_organization_service.academic_option.dto.CreateAcademicOptionDto;
import eclosia.eclosia_organization_service.academic_option.dto.UpdateAcademicOptionDto;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_option.service.AcademicOptionService;
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
@RequestMapping(path = "academic-option")
@RequiredArgsConstructor
public class AcademicOptionController {

    private final AcademicOptionService service;

    @PostMapping
    public ResponseEntity<AcademicOption> create(@Valid @RequestBody CreateAcademicOptionDto dto) {
        AcademicOption academicOption = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicOption);
    }

    @GetMapping
    public List<AcademicOption> findAll(
            @RequestParam(required = false) UUID academicSectionId
    ) {
        if (academicSectionId != null) {
            return service.findByAcademicSectionId(academicSectionId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicOption findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicOption update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicOptionDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
