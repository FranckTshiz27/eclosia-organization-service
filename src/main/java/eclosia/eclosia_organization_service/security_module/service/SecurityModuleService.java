package eclosia.eclosia_organization_service.security_module.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.security_module.dto.CreateSecurityModuleDto;
import eclosia.eclosia_organization_service.security_module.dto.UpdateSecurityModuleDto;
import eclosia.eclosia_organization_service.security_module.entity.SecurityModule;
import eclosia.eclosia_organization_service.security_module.repository.SecurityModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityModuleService {

    private final SecurityModuleRepository repository;

    public SecurityModule create(CreateSecurityModuleDto dto) {
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, null);

        SecurityModule module = new SecurityModule();
        mapFromDto(
                module,
                code,
                dto.getName(),
                dto.getDescription(),
                dto.getIcon(),
                dto.getDisplayOrder(),
                dto.getActive(),
                dto.getSystemModule()
        );
        return repository.save(module);
    }

    public List<SecurityModule> findAll() {
        return repository.findAllByOrderByDisplayOrderAscCodeAsc();
    }

    public SecurityModule findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Security module not found"));
    }

    public SecurityModule update(UUID id, UpdateSecurityModuleDto dto) {
        SecurityModule module = findById(id);
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, id);

        mapFromDto(
                module,
                code,
                dto.getName(),
                dto.getDescription(),
                dto.getIcon(),
                dto.getDisplayOrder(),
                dto.getActive(),
                dto.getSystemModule()
        );
        return repository.save(module);
    }

    public void delete(UUID id) {
        SecurityModule module = findById(id);
        if (Boolean.TRUE.equals(module.getSystemModule())) {
            throw new BadRequestException("System modules cannot be deleted");
        }
        repository.delete(module);
    }

    private void validateUniqueness(String code, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByCode(code)) {
                throw new BadRequestException("Security module code already exists");
            }
            return;
        }

        if (repository.existsByCodeAndIdNot(code, excludeId)) {
            throw new BadRequestException("Security module code already exists");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private void mapFromDto(
            SecurityModule module,
            String code,
            String name,
            String description,
            String icon,
            Integer displayOrder,
            Boolean active,
            Boolean systemModule
    ) {
        module.setCode(code);
        module.setName(name);
        module.setDescription(description);
        module.setIcon(icon);
        module.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        module.setActive(active != null ? active : true);
        module.setSystemModule(systemModule != null ? systemModule : true);
    }
}
