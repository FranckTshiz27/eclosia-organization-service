package eclosia.eclosia_organization_service.student_grade.controller;

import eclosia.eclosia_organization_service.student_grade.dto.CreateStudentGradeDto;
import eclosia.eclosia_organization_service.student_grade.dto.UpdateStudentGradeDto;
import eclosia.eclosia_organization_service.student_grade.entity.StudentGrade;
import eclosia.eclosia_organization_service.student_grade.service.StudentGradeService;
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
@RequestMapping(path = "student-grade")
@RequiredArgsConstructor
public class StudentGradeController {

    private final StudentGradeService service;

    @PostMapping
    public ResponseEntity<StudentGrade> create(@Valid @RequestBody CreateStudentGradeDto dto) {
        StudentGrade studentGrade = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(studentGrade);
    }

    @GetMapping
    public List<StudentGrade> findAll(
            @RequestParam(required = false) UUID studentEnrollmentId,
            @RequestParam(required = false) UUID academicPeriodId,
            @RequestParam(required = false) UUID academicCurriculumSubjectId
    ) {
        if (studentEnrollmentId != null && academicPeriodId != null) {
            return service.findByStudentEnrollmentIdAndAcademicPeriodId(
                    studentEnrollmentId,
                    academicPeriodId
            );
        }
        if (studentEnrollmentId != null) {
            return service.findByStudentEnrollmentId(studentEnrollmentId);
        }
        if (academicPeriodId != null) {
            return service.findByAcademicPeriodId(academicPeriodId);
        }
        if (academicCurriculumSubjectId != null) {
            return service.findByAcademicCurriculumSubjectId(academicCurriculumSubjectId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public StudentGrade findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public StudentGrade update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentGradeDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
