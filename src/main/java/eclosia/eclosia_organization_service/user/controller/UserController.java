package eclosia.eclosia_organization_service.user.controller;

import eclosia.eclosia_organization_service.user.dto.CreateUserDto;
import eclosia.eclosia_organization_service.user.dto.UpdateUserDto;
import eclosia.eclosia_organization_service.user.entity.User;
import eclosia.eclosia_organization_service.user.service.UserService;
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
@RequestMapping(path = "user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserDto dto) {
        User user = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public List<User> findAll(
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID roleId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        if (groupId != null) {
            return service.findByGroupId(groupId);
        }
        if (roleId != null) {
            return service.findByRoleId(roleId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/by-keycloak/{keycloakId}")
    public User findByKeycloakId(@PathVariable UUID keycloakId) {
        return service.findByKeycloakId(keycloakId);
    }

    @PutMapping("/{id}")
    public User update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
