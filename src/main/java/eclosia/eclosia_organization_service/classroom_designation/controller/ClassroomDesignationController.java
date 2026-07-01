package eclosia.eclosia_organization_service.classroom_designation.controller;

import eclosia.eclosia_organization_service.classroom_designation.dto.CreateClassroomDesignationDto;
import eclosia.eclosia_organization_service.classroom_designation.dto.UpdateClassroomDesignationDto;
import eclosia.eclosia_organization_service.classroom_designation.entity.ClassroomDesignation;
import eclosia.eclosia_organization_service.classroom_designation.service.ClassroomDesignationService;
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
@RequestMapping(path = "classroom-designation")
@RequiredArgsConstructor
public class ClassroomDesignationController {

    private final ClassroomDesignationService service;

    @PostMapping
    public ResponseEntity<ClassroomDesignation> create(@Valid @RequestBody CreateClassroomDesignationDto dto) {
        ClassroomDesignation designation = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(designation);
    }

    @GetMapping
    public List<ClassroomDesignation> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ClassroomDesignation findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public ClassroomDesignation update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassroomDesignationDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
