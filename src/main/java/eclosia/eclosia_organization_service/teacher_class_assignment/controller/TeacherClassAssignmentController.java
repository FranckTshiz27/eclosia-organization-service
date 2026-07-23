package eclosia.eclosia_organization_service.teacher_class_assignment.controller;

import eclosia.eclosia_organization_service.teacher_class_assignment.dto.CreateTeacherClassAssignmentDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.dto.TeacherClassAssignmentResponseDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.dto.UpdateTeacherClassAssignmentDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.service.TeacherClassAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping(path = "teacher-class-assignments")
@RequiredArgsConstructor
public class TeacherClassAssignmentController {

    private final TeacherClassAssignmentService service;

    @PostMapping
    public ResponseEntity<TeacherClassAssignmentResponseDto> create(
            @Valid @RequestBody CreateTeacherClassAssignmentDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/{id}")
    public TeacherClassAssignmentResponseDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public TeacherClassAssignmentResponseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeacherClassAssignmentDto dto
    ) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/deactivate")
    public TeacherClassAssignmentResponseDto deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @GetMapping
    public List<TeacherClassAssignmentResponseDto> findBySchoolAndYear(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId
    ) {
        return service.findBySchoolAndYear(schoolId, academicYearId);
    }

    @GetMapping("/classroom/{classroomId}")
    public TeacherClassAssignmentResponseDto findByClassroom(
            @PathVariable UUID classroomId,
            @RequestParam UUID academicYearId
    ) {
        return service.findActiveByClassroomAndYear(classroomId, academicYearId);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<TeacherClassAssignmentResponseDto> findByTeacher(
            @PathVariable UUID teacherId,
            @RequestParam(required = false) UUID academicYearId
    ) {
        return service.findByTeacher(teacherId, academicYearId);
    }
}
