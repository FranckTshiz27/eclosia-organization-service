package eclosia.eclosia_organization_service.security_module.controller;

import eclosia.eclosia_organization_service.security_module.dto.CreateSecurityModuleDto;
import eclosia.eclosia_organization_service.security_module.dto.UpdateSecurityModuleDto;
import eclosia.eclosia_organization_service.security_module.entity.SecurityModule;
import eclosia.eclosia_organization_service.security_module.service.SecurityModuleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "security-module")
@RequiredArgsConstructor
public class SecurityModuleController {

    private final SecurityModuleService service;

    @PostMapping
    public ResponseEntity<SecurityModule> create(@Valid @RequestBody CreateSecurityModuleDto dto) {
        SecurityModule module = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(module);
    }

    @GetMapping
    public List<SecurityModule> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SecurityModule findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public SecurityModule update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSecurityModuleDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
