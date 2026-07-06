package eclosia.eclosia_organization_service.enrollment.controller;

import eclosia.eclosia_organization_service.common.dto.PagedResponseDto;
import eclosia.eclosia_organization_service.enrollment.dto.CreateEnrollmentDto;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Enrollment> create(
            @Valid @ModelAttribute CreateEnrollmentDto dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        Enrollment enrollment = service.create(dto, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @GetMapping
    public PagedResponseDto<Enrollment> findAll(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classroomId,
            @RequestParam(required = false) UUID guardianId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Enrollment> enrollments = service.findAll(academicYearId, classroomId, guardianId, studentId, page, size);
        return PagedResponseDto.from(enrollments);
    }

    @GetMapping("/by-academic-year-and-school")
    public PagedResponseDto<Enrollment> findByAcademicYearAndSchool(
            @RequestParam UUID academicYearId,
            @RequestParam UUID schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        Page<Enrollment> enrollments = service.findByAcademicYearAndSchool(academicYearId, schoolId, page, size);
        return PagedResponseDto.from(enrollments);
    }

    @GetMapping("/search")
    public List<Enrollment> searchByStudentName(
            @RequestParam String name,
            @RequestParam UUID academicYearId,
            @RequestParam UUID schoolId
    ) {
        return service.searchByStudentNameAndAcademicYearAndSchool(name, academicYearId, schoolId);
    }

    @GetMapping("/{id}")
    public Enrollment findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
