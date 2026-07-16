package eclosia.eclosia_organization_service.academic_period.controller;

import eclosia.eclosia_organization_service.academic_period.dto.CreateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.dto.UpdateAcademicPeriodDto;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.academic_period.service.AcademicPeriodService;
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
@RequestMapping(path = "academic-period")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService service;

    @PostMapping
    public ResponseEntity<AcademicPeriod> create(@Valid @RequestBody CreateAcademicPeriodDto dto) {
        AcademicPeriod academicPeriod = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(academicPeriod);
    }

    @GetMapping
    public List<AcademicPeriod> findAll(@RequestParam(required = false) UUID countryId) {
        if (countryId != null) {
            return service.findByCountryId(countryId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicPeriod findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicPeriod update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicPeriodDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
