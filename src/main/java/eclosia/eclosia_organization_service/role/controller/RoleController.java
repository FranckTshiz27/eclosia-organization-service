package eclosia.eclosia_organization_service.role.controller;

import eclosia.eclosia_organization_service.role.dto.CreateRoleDto;
import eclosia.eclosia_organization_service.role.dto.UpdateRoleDto;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role.service.RoleService;
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
@RequestMapping(path = "role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService service;

    @PostMapping
    public ResponseEntity<Role> create(@Valid @RequestBody CreateRoleDto dto) {
        Role role = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @GetMapping
    public List<Role> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Role findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Role update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
