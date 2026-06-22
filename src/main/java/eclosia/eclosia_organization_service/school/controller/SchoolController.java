package eclosia.eclosia_organization_service.school.controller;

import eclosia.eclosia_organization_service.school.dto.CreateSchoolDto;
import eclosia.eclosia_organization_service.school.dto.UpdateSchoolDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.service.SchoolService;
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
@RequestMapping(path = "school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService service;

    @PostMapping
    public ResponseEntity<School> create(@Valid @RequestBody CreateSchoolDto dto) {
        School school = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(school);
    }

    @GetMapping
    public List<School> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public School findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public School update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
