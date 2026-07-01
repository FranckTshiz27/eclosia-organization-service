package eclosia.eclosia_organization_service.classroom.controller;

import eclosia.eclosia_organization_service.classroom.dto.CreateClassroomDto;
import eclosia.eclosia_organization_service.classroom.dto.UpdateClassroomDto;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.service.ClassroomService;
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
@RequestMapping(path = "classroom")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService service;

    @PostMapping
    public ResponseEntity<Classroom> create(@Valid @RequestBody CreateClassroomDto dto) {
        Classroom classroom = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(classroom);
    }

    @GetMapping
    public List<Classroom> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Classroom findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Classroom update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassroomDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
