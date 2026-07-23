package eclosia.eclosia_organization_service.role.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.role.dto.CreateRoleDto;
import eclosia.eclosia_organization_service.role.dto.UpdateRoleDto;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repository;

    public Role create(CreateRoleDto dto) {
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, null);

        Role role = new Role();
        mapFromDto(
                role,
                code,
                dto.getName(),
                dto.getDescription(),
                dto.getSystemRole(),
                dto.getActive(),
                dto.getDisplayOrder()
        );
        return repository.save(role);
    }

    public List<Role> findAll() {
        return repository.findAllByOrderByDisplayOrderAscCodeAsc();
    }

    public Role findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    public Role update(UUID id, UpdateRoleDto dto) {
        Role role = findById(id);
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, id);

        mapFromDto(
                role,
                code,
                dto.getName(),
                dto.getDescription(),
                dto.getSystemRole(),
                dto.getActive(),
                dto.getDisplayOrder()
        );
        return repository.save(role);
    }

    public void delete(UUID id) {
        Role role = findById(id);
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new BadRequestException("System roles cannot be deleted");
        }
        repository.delete(role);
    }

    private void validateUniqueness(String code, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByCode(code)) {
                throw new BadRequestException("Role code already exists");
            }
            return;
        }

        if (repository.existsByCodeAndIdNot(code, excludeId)) {
            throw new BadRequestException("Role code already exists");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private void mapFromDto(
            Role role,
            String code,
            String name,
            String description,
            Boolean systemRole,
            Boolean active,
            Integer displayOrder
    ) {
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setSystemRole(systemRole != null ? systemRole : true);
        role.setActive(active != null ? active : true);
        role.setDisplayOrder(displayOrder != null ? displayOrder : 0);
    }
}
