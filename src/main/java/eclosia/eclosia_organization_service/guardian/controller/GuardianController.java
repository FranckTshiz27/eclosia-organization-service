package eclosia.eclosia_organization_service.guardian.controller;

import eclosia.eclosia_organization_service.guardian.dto.CreateGuardianDto;
import eclosia.eclosia_organization_service.guardian.dto.UpdateGuardianDto;
import eclosia.eclosia_organization_service.guardian.entity.Guardian;
import eclosia.eclosia_organization_service.guardian.service.GuardianService;
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
@RequestMapping(path = "guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService service;

    @PostMapping
    public ResponseEntity<Guardian> create(@Valid @RequestBody CreateGuardianDto dto) {
        Guardian guardian = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardian);
    }

    @GetMapping
    public List<Guardian> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Guardian findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Guardian update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGuardianDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
