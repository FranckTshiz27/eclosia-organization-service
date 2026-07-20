package eclosia.eclosia_organization_service.academic_curriculum_subject.controller;

import eclosia.eclosia_organization_service.academic_curriculum_subject.dto.CreateAcademicCurriculumSubjectDto;
import eclosia.eclosia_organization_service.academic_curriculum_subject.dto.UpdateAcademicCurriculumSubjectDto;
import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import eclosia.eclosia_organization_service.academic_curriculum_subject.service.AcademicCurriculumSubjectService;
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
@RequestMapping(path = "academic-curriculum-subject")
@RequiredArgsConstructor
public class AcademicCurriculumSubjectController {

    private final AcademicCurriculumSubjectService service;

    @PostMapping
    public ResponseEntity<AcademicCurriculumSubject> create(
            @Valid @RequestBody CreateAcademicCurriculumSubjectDto dto
    ) {
        AcademicCurriculumSubject academicCurriculumSubject = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicCurriculumSubject);
    }

    @GetMapping
    public List<AcademicCurriculumSubject> findAll(
            @RequestParam(required = false) UUID academicCurriculumId,
            @RequestParam(required = false) UUID subjectId
    ) {
        if (academicCurriculumId != null) {
            return service.findByAcademicCurriculumId(academicCurriculumId);
        }
        if (subjectId != null) {
            return service.findBySubjectId(subjectId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicCurriculumSubject findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicCurriculumSubject update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicCurriculumSubjectDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
