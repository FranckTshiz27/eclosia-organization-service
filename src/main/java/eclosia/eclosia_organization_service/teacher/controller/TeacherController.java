package eclosia.eclosia_organization_service.teacher.controller;

import eclosia.eclosia_organization_service.teacher.dto.CreateTeacherDto;
import eclosia.eclosia_organization_service.teacher.dto.TeacherResponseDto;
import eclosia.eclosia_organization_service.teacher.dto.UpdateTeacherDto;
import eclosia.eclosia_organization_service.teacher.service.TeacherService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    public ResponseEntity<TeacherResponseDto> create(@Valid @RequestBody CreateTeacherDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(dto));
    }

    @GetMapping
    public List<TeacherResponseDto> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return teacherService.findBySchoolId(schoolId);
        }
        return teacherService.findAll();
    }

    @GetMapping("/me")
    public TeacherResponseDto me() {
        return teacherService.findCurrentTeacher();
    }

    @GetMapping("/{id}")
    public TeacherResponseDto findById(@PathVariable UUID id) {
        return teacherService.findById(id);
    }

    @PutMapping("/{id}")
    public TeacherResponseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeacherDto dto
    ) {
        return teacherService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teacherService.delete(id);
    }
}
