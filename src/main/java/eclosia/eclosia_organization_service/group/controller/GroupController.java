package eclosia.eclosia_organization_service.group.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eclosia.eclosia_organization_service.group.dto.CreateGroupDto;
import eclosia.eclosia_organization_service.group.dto.UpdateGroupDto;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.group.service.GroupService;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService service;

    @PostMapping
    public ResponseEntity<Group> create(@Valid @RequestBody CreateGroupDto dto) {
        Group group = service.create(dto);
         return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public List<Group> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Group findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Group update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}