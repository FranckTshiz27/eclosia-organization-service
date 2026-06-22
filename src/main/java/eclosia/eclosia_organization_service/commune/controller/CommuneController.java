package eclosia.eclosia_organization_service.commune.controller;

import eclosia.eclosia_organization_service.commune.dto.CreateCommuneDto;
import eclosia.eclosia_organization_service.commune.dto.UpdateCommuneDto;
import eclosia.eclosia_organization_service.commune.entity.Commune;
import eclosia.eclosia_organization_service.commune.service.CommuneService;
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
@RequestMapping(path = "commune")
@RequiredArgsConstructor
public class CommuneController {

    private final CommuneService service;

    @PostMapping
    public ResponseEntity<Commune> create(@Valid @RequestBody CreateCommuneDto dto) {
        Commune commune = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(commune);
    }

    @GetMapping
    public List<Commune> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Commune findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Commune update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommuneDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
