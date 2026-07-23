package eclosia.eclosia_organization_service.teacher_course_assignment.controller;

import eclosia.eclosia_organization_service.teacher_course_assignment.dto.CreateTeacherCourseAssignmentBatch;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.TeacherCourseAssignmentResponseDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.UpdateTeacherCourseAssignmentDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.service.TeacherCourseAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping(path = "teacher-course-assignments")
@RequiredArgsConstructor
@Validated
public class TeacherCourseAssignmentController {

    private final TeacherCourseAssignmentService service;

    /**
     * Accepte un objet unique ou un tableau (même pattern que les paiements).
     */
    @PostMapping
    public ResponseEntity<List<TeacherCourseAssignmentResponseDto>> create(
            @NotEmpty(message = "At least one course assignment is required")
            @RequestBody CreateTeacherCourseAssignmentBatch dtos
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAll(dtos));
    }

    @GetMapping("/{id}")
    public TeacherCourseAssignmentResponseDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public TeacherCourseAssignmentResponseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeacherCourseAssignmentDto dto
    ) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/deactivate")
    public TeacherCourseAssignmentResponseDto deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<TeacherCourseAssignmentResponseDto> findByTeacher(
            @PathVariable UUID teacherId,
            @RequestParam(required = false) UUID academicYearId
    ) {
        return service.findByTeacher(teacherId, academicYearId);
    }

    @GetMapping("/classroom/{classroomId}")
    public List<TeacherCourseAssignmentResponseDto> findByClassroom(
            @PathVariable UUID classroomId,
            @RequestParam UUID academicYearId
    ) {
        return service.findByClassroom(classroomId, academicYearId);
    }

    @GetMapping("/subject/{subjectId}")
    public List<TeacherCourseAssignmentResponseDto> findBySubject(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID academicYearId
    ) {
        return service.findBySubject(subjectId, schoolId, academicYearId);
    }

    @GetMapping
    public List<TeacherCourseAssignmentResponseDto> findByYear(
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) UUID schoolId
    ) {
        return service.findByYearAndSchool(academicYearId, schoolId);
    }
}
