package eclosia.eclosia_organization_service.academic_fee.controller;

import eclosia.eclosia_organization_service.academic_fee.dto.CreateAcademicFeeDto;
import eclosia.eclosia_organization_service.academic_fee.dto.UpdateAcademicFeeDto;
import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.service.AcademicFeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping(path = "academic-fee")
@RequiredArgsConstructor
@Validated
public class AcademicFeeController {

    private final AcademicFeeService service;

    @PostMapping
    public ResponseEntity<List<AcademicFee>> create(
            @NotEmpty(message = "At least one academic fee is required")
            @Valid @RequestBody List<@Valid CreateAcademicFeeDto> dtos
    ) {
        List<AcademicFee> academicFees = service.createAll(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicFees);
    }

    @GetMapping
    public List<AcademicFee> findAll(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID enrollmentId
    ) {
        if (enrollmentId != null) {
            return service.findByEnrollmentId(enrollmentId);
        }
        if (schoolId != null && academicYearId != null) {
            return service.findBySchoolIdAndAcademicYearId(schoolId, academicYearId);
        }
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    public List<AcademicFee> findByEnrollment(@PathVariable UUID enrollmentId) {
        return service.findByEnrollmentId(enrollmentId);
    }

    @GetMapping("/{id}")
    public AcademicFee findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicFee update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicFeeDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
