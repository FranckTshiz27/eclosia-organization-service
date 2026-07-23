package eclosia.eclosia_organization_service.role_feature.controller;

import eclosia.eclosia_organization_service.role_feature.dto.CreateRoleFeatureDto;
import eclosia.eclosia_organization_service.role_feature.dto.UpdateRoleFeatureDto;
import eclosia.eclosia_organization_service.role_feature.entity.RoleFeature;
import eclosia.eclosia_organization_service.role_feature.service.RoleFeatureService;
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
@RequestMapping(path = "role-feature")
@RequiredArgsConstructor
@Validated
public class RoleFeatureController {

    private final RoleFeatureService service;

    @PostMapping
    public ResponseEntity<List<RoleFeature>> create(
            @NotEmpty(message = "At least one role feature is required")
            @Valid @RequestBody List<@Valid CreateRoleFeatureDto> dtos
    ) {
        List<RoleFeature> roleFeatures = service.createAll(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleFeatures);
    }

    @GetMapping
    public List<RoleFeature> findAll(
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) UUID featureId
    ) {
        if (roleId != null) {
            return service.findByRoleId(roleId);
        }
        if (featureId != null) {
            return service.findByFeatureId(featureId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RoleFeature findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public RoleFeature update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleFeatureDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
