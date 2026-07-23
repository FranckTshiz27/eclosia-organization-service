package eclosia.eclosia_organization_service.feature.controller;

import eclosia.eclosia_organization_service.feature.dto.CreateFeatureDto;
import eclosia.eclosia_organization_service.feature.dto.UpdateFeatureDto;
import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.feature.service.FeatureService;
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
@RequestMapping(path = "feature")
@RequiredArgsConstructor
@Validated
public class FeatureController {

    private final FeatureService service;

    @PostMapping
    public ResponseEntity<List<Feature>> create(
            @NotEmpty(message = "At least one feature is required")
            @Valid @RequestBody List<@Valid CreateFeatureDto> dtos
    ) {
        List<Feature> features = service.createAll(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(features);
    }

    @GetMapping
    public List<Feature> findAll(
            @RequestParam(required = false) UUID moduleId
    ) {
        if (moduleId != null) {
            return service.findByModuleId(moduleId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Feature findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Feature update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeatureDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
