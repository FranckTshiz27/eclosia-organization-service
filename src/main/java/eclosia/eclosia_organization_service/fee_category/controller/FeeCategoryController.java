package eclosia.eclosia_organization_service.fee_category.controller;

import eclosia.eclosia_organization_service.fee_category.dto.CreateFeeCategoryDto;
import eclosia.eclosia_organization_service.fee_category.dto.UpdateFeeCategoryDto;
import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import eclosia.eclosia_organization_service.fee_category.service.FeeCategoryService;
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
@RequestMapping(path = "fee-category")
@RequiredArgsConstructor
public class FeeCategoryController {

    private final FeeCategoryService service;

    @PostMapping
    public ResponseEntity<FeeCategory> create(@Valid @RequestBody CreateFeeCategoryDto dto) {
        FeeCategory feeCategory = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(feeCategory);
    }

    @GetMapping
    public List<FeeCategory> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public FeeCategory findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public FeeCategory update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeeCategoryDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
